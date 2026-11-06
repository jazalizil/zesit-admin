package fr.zesit.admin.ntfy

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) { json() }
    install(Logging) { level = LogLevel.INFO }
    engine {
        // WebSockets natifs gérés par Darwin (NSURLSession)
    }
}
