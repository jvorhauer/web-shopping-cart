package nl.miruvor

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.html.body
import kotlinx.html.h1
import kotlinx.html.li
import kotlinx.html.ul
import org.slf4j.event.Level

fun Application.configureHTTP() {
  install(DefaultHeaders) {
    header("X-Engine", "Ktor") // will send this header with each response
  }
}

fun Application.configureMonitoring() {
  install(CallLogging) {
    level = Level.INFO
    filter { call -> call.request.path().startsWith("/") }
  }

  val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

  install(MicrometerMetrics) {
    registry = appMicrometerRegistry
    // ...
  }

  routing {
    get("/metrics-micrometer") {
      call.respond(appMicrometerRegistry.scrape())
    }
  }
}

fun Application.configureSecurity() {}

fun Application.configureSerialization() {
  install(ContentNegotiation) {
    json()
  }

  routing {
    get("/json/kotlinx-serialization") {
      call.respond(mapOf("hello" to "world"))
    }
  }
}

fun Application.configureTemplating() {
  routing {
    get("/html-dsl") {
      call.respondHtml {
        body {
          h1 { +"HTML" }
          ul {
            for (n in 1..10) {
              li { +"$n" }
            }
          }
        }
      }
    }
  }
}
