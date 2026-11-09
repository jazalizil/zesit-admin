package fr.zesit.admin.traffic

import fr.zesit.admin.ntfy.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal class KtorTrafficApi(private val client: HttpClient) : TrafficApi {

    private fun HttpRequestBuilder.bearer(token: String?) {
        token?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    private fun url(baseUrl: String, path: String, args: Map<String, Any?> = emptyMap()): String {
        val base = baseUrl.trimEnd('/') + path
        val qs = args.filterValues { it != null }.entries
            .joinToString("&") { entry -> "${entry.key}=${entry.value.toString().encodeURLParameter()}" }
        return if (qs.isEmpty()) base else "$base?$qs"
    }

    private suspend fun call(
        baseUrl: String,
        path: String,
        args: Map<String, Any?> = emptyMap(),
        token: String? = null,
    ): HttpResponse = client.get(url(baseUrl, path, args)) { bearer(token) }

    /** 401 -> [TrafficAuthException] ; 404 -> null ; autre erreur -> [TrafficApiException]. Le 404 permet de distinguer l'id absent ([TrafficApi.request]) de l'API désactivée ([fetch]). */
    private suspend fun ensureOk(response: HttpResponse, what: String): HttpResponse? = when {
        response.status == HttpStatusCode.Unauthorized -> throw TrafficAuthException()
        response.status == HttpStatusCode.NotFound -> null
        !response.status.isSuccess() ->
            throw TrafficApiException("$what — HTTP ${response.status.value} : ${response.bodyAsText().take(200).ifBlank { "—" }}")
        else -> response
    }

    private suspend fun fetch(
        baseUrl: String,
        path: String,
        token: String,
        args: Map<String, Any?> = emptyMap(),
        what: String,
    ): HttpResponse =
        ensureOk(call(baseUrl, path, args, token), what) ?: throw TrafficApiDisabledException()

    private suspend fun <T> decode(response: HttpResponse, serializer: DeserializationStrategy<T>): T =
        json.decodeFromString(serializer, response.bodyAsText())

    override suspend fun health(baseUrl: String): TrafficHealth {
        // Endpoint public : pas de token, aucune auth à vérifier.
        val response = call(baseUrl, "/ti-api/health")
        val ok = ensureOk(response, "Health traffic-inspector")
            ?: throw TrafficApiException("Health traffic-inspector — 404 inattendu")
        return decode(ok, TrafficHealth.serializer())
    }

    override suspend fun checkAuth(baseUrl: String, token: String) {
        // 404 = API désactivée côté serveur ; 401 = token invalide.
        fetch(baseUrl, "/ti-api/auth", token, what = "Auth traffic-inspector")
    }

    override suspend fun stats(
        baseUrl: String,
        token: String,
        hours: Double,
    ): TrafficStatsEnvelope =
        decode(
            fetch(baseUrl, "/ti-api/stats", token, mapOf("hours" to hours), "Stats traffic"),
            TrafficStatsEnvelope.serializer(),
        )

    override suspend fun requests(
        baseUrl: String,
        token: String,
        query: TrafficQuery,
    ): TrafficRequestList {
        val args = buildMap<String, Any?> {
            put("hours", query.hours)
            put("limit", query.limit)
            put("offset", query.offset)
            query.ip?.let { put("ip", it) }
            query.method?.let { put("method", it) }
            query.source?.let { put("source", it) }
            query.suspicious?.let { put("suspicious", it) }
            query.status?.let { put("status", it) }
            query.search?.let { put("search", it) }
        }
        return decode(
            fetch(baseUrl, "/ti-api/requests", token, args, "Journal traffic"),
            TrafficRequestList.serializer(),
        )
    }

    override suspend fun request(baseUrl: String, token: String, id: Long): TrafficRequestDetail? {
        // 404 : id absent (ou API désactivée — distingué au premier appel par checkAuth/health).
        val response = ensureOk(call(baseUrl, "/ti-api/requests/$id", token = token), "Détail traffic")
            ?: return null
        return decode(response, TrafficRequestDetail.serializer())
    }

    override suspend fun ip(baseUrl: String, token: String, ip: String, limit: Int): TrafficIpSheet =
        decode(
            fetch(
                baseUrl,
                "/ti-api/ip/${ip.encodeURLParameter()}",
                token,
                mapOf("limit" to limit),
                "Fiche IP traffic",
            ),
            TrafficIpSheet.serializer(),
        )

    override suspend fun alerts(baseUrl: String, token: String, hours: Double, limit: Int): TrafficAlerts =
        decode(
            fetch(
                baseUrl,
                "/ti-api/alerts",
                token,
                mapOf("hours" to hours, "limit" to limit),
                "Alertes traffic",
            ),
            TrafficAlerts.serializer(),
        )
}
