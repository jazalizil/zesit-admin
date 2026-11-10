package fr.zesit.admin.security

import fr.zesit.admin.ntfy.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal class KtorSecurityApi(private val client: HttpClient) : SecurityApi {

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
    ): HttpResponse = client.get(url(baseUrl, path, args)) {
        token?.takeIf { it.isNotBlank() }?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    /** 401 -> [SecurityAuthException] ; 404 -> null ; autre erreur -> [SecurityApiException]. */
    private suspend fun ensureOk(response: HttpResponse, what: String): HttpResponse? = when {
        response.status == HttpStatusCode.Unauthorized -> throw SecurityAuthException()
        response.status == HttpStatusCode.NotFound -> null
        !response.status.isSuccess() ->
            throw SecurityApiException("$what — HTTP ${response.status.value} : ${response.bodyAsText().take(200).ifBlank { "—" }}")
        else -> response
    }

    private suspend fun fetch(
        baseUrl: String,
        path: String,
        token: String,
        args: Map<String, Any?> = emptyMap(),
        what: String,
    ): HttpResponse =
        ensureOk(call(baseUrl, path, args, token), what) ?: throw SecurityApiDisabledException()

    private suspend fun <T> decode(
        response: HttpResponse,
        serializer: kotlinx.serialization.DeserializationStrategy<T>,
    ): T = json.decodeFromString(serializer, response.bodyAsText())

    override suspend fun health(baseUrl: String): SecurityHealth {
        // Endpoint public : pas de token, aucune auth à vérifier.
        val response = ensureOk(call(baseUrl, "/sc-api/health"), "Health security-scanner")
            ?: throw SecurityApiException("Health security-scanner — 404 inattendu")
        return decode(response, SecurityHealth.serializer())
    }

    override suspend fun summary(baseUrl: String, token: String): SecuritySummary =
        decode(
            fetch(baseUrl, "/sc-api/summary", token, what = "Résumé sécurité"),
            SecuritySummary.serializer(),
        )

    override suspend fun findings(
        baseUrl: String,
        token: String,
        image: String,
        severity: String?,
        q: String?,
        limit: Int,
        offset: Int,
    ): SecurityFindingsList {
        val args = buildMap<String, Any?> {
            put("image", image)
            put("limit", limit)
            put("offset", offset)
            severity?.let { put("severity", it) }
            q?.let { put("q", it) }
        }
        return decode(
            fetch(baseUrl, "/sc-api/findings", token, args, "Vulnérabilités"),
            SecurityFindingsList.serializer(),
        )
    }

    override suspend fun scans(baseUrl: String, token: String, limit: Int, offset: Int): SecurityScans =
        decode(
            fetch(baseUrl, "/sc-api/scans", token, mapOf("limit" to limit, "offset" to offset), "Historique des scans"),
            SecurityScans.serializer(),
        )

    override suspend fun triggerScan(baseUrl: String, token: String): Long {
        // POST sans body : 202 {queued, run_id} ; 409 = un scan est déjà en cours ;
        // 401 = token refusé ; 404 = API désactivée.
        val response = client.post(url(baseUrl, "/sc-api/scan")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        when {
            response.status == HttpStatusCode.Unauthorized -> throw SecurityAuthException()
            response.status == HttpStatusCode.Conflict -> throw SecurityScanConflictException()
            response.status == HttpStatusCode.NotFound -> throw SecurityApiDisabledException()
            !response.status.isSuccess() ->
                throw SecurityApiException("Scan manuel — HTTP ${response.status.value}")
        }
        return json.decodeFromString(TriggerScanResponse.serializer(), response.bodyAsText()).run_id
    }

    @kotlinx.serialization.Serializable
    private data class TriggerScanResponse(val queued: Boolean = false, val run_id: Long = 0)
}
