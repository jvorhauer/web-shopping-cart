package nl.miruvor

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import nl.miruvor.plugins.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureMonitoring()
        configureTemplating()
        configureSerialization()
        configureHTTP()
        configureSecurity()
        configureRouting()
    }.start(wait = true)
}
