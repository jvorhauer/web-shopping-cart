package nl.miruvor

import akka.actor.testkit.typed.javadsl.TestKitJunitResource
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

private val testKit = TestKitJunitResource(
  """akka.persistence.journal.plugin = "akka.persistence.journal.inmem" 
       akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"  
       akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID()}"  
    """
)

class ApplicationTest {
  @Test
  fun `test routing`() = testApplication {
    application {
      configureRouting(testKit.spawn(ShoppingCart.create("cart-1")), testKit.system())
    }
    client.get("/").apply {
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("Hello World!", bodyAsText())
    }
    client.post("/double-receive") {
      contentType(ContentType.Application.Json)
      setBody("test")
    }.apply {
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("test test", bodyAsText())
    }
  }

  @Test
  fun `test json serialization`() = testApplication {
    application {
      configureRouting(testKit.spawn(ShoppingCart.create("cart-2")), testKit.system())
      configureSerialization()
    }
    client.get("/json/kotlinx-serialization").apply {
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("""{"hello":"world"}""", bodyAsText())
    }
  }

  @Test
  fun `persistence route with foo`() = testApplication {
    application {
      configureRouting(testKit.spawn(ShoppingCart.create("cart-1")), testKit.system())
      configureSerialization()
    }
    client.post("/cart") {
      contentType(ContentType.Application.Json)
      setBody("foo")
    }.apply {
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("""{"items":{"foo":1},"checkedOut":false}""", bodyAsText())
    }
    client.get("/cart").apply {
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("""{"items":{"foo":1},"checkedOut":false}""", bodyAsText())
    }
    client.post("/checkout").apply {
      assertEquals(HttpStatusCode.OK, status)
      assertEquals("""{"items":{"foo":1},"checkedOut":true}""", bodyAsText())
    }
  }
}
