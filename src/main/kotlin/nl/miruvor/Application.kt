package nl.miruvor

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.javadsl.AskPattern
import akka.actor.typed.javadsl.Behaviors
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.config.validate.ValidationException
import kotlinx.coroutines.future.await
import java.time.Duration

fun main() {
  ActorSystem.create(Server.create("0.0.0.0", 8080), "cart-sys")
}

interface Message
class Started : Message
class StartFailed: Message
class Stop : Message

object Server {
  fun create(host: String, port: Int): Behavior<Message> =
    Behaviors.setup { ctx ->
      val cart = ctx.spawn(ShoppingCart.create("web-cart"), "cart")
      embeddedServer(Netty, port = port, host = host) {
        configureMonitoring()
        configureTemplating()
        configureSerialization()
        configureHTTP()
        configureSecurity()
        configureRouting(cart, ctx.system)
      }.start(wait = true)
      Behaviors.empty()
    }
}

fun Application.configureRouting(cart: ActorRef<Command>, system: ActorSystem<Void>) {
  install(DoubleReceive)
  install(RequestValidation)
  install(StatusPages) {
    exception<AuthenticationException> { call, _ ->
      call.respond(HttpStatusCode.Unauthorized)
    }
    exception<AuthorizationException> { call, _ ->
      call.respond(HttpStatusCode.Forbidden)
    }
    exception<ValidationException> { call, cause ->
      call.respond(HttpStatusCode.BadRequest, cause.message ?: "Unknown reason")
    }
  }

  val timeout = Duration.ofSeconds(1)
  val scheduler = system.scheduler()

  routing {
    get("/") {
      call.respondText("Hello World!")
    }
    post("/double-receive") {
      val first = call.receiveText()
      val theSame = call.receiveText()
      call.respondText("$first $theSame")
    }
    post("/cart") {
      val item = call.receiveText()
      val fut = AskPattern.ask(cart, { replyTo -> AddItem(item, 1, replyTo) }, timeout, scheduler)
      call.respond(fut.await().value)
    }
    get("/cart") {
      val fut = AskPattern.ask(cart, { replyTo -> Get(replyTo) }, timeout, scheduler)
      call.respond(fut.await().value)
    }
    post("/checkout") {
      val fut = AskPattern.ask(cart, { rt -> Checkout(rt) }, timeout, scheduler)
      call.respond(fut.await().value)
    }
  }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
