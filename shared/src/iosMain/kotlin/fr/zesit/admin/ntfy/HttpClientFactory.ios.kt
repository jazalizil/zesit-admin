package fr.zesit.admin.ntfy

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json

internal actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    install(ContentNegotiation) { json() }
    // Sans timeout explicite, un serveur joignable-muet (VPN mi-route, réseau
    // captif, IP non routable) fait pendre l'app à l'infini au lieu de
    // remonter une erreur. 12s pour établir la connexion, 30s pour la requête
    // (le WebSocket ntfy reste valide : le timeout porte sur l'appel HTTP).
    install(HttpTimeout) {
        connectTimeoutMillis = 12_000
        requestTimeoutMillis = 30_000
    }
    install(Logging) { level = LogLevel.INFO }
    engine {
        // WebSockets natifs gérés par Darwin (NSURLSession)
    }
}
