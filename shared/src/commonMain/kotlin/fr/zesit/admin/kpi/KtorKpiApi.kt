package fr.zesit.admin.kpi

import fr.zesit.admin.ntfy.createHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.setCookie
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
private data class LoginRequest(val username: String, val password: String)

@Serializable
private data class MeResponse(val user: String)

@Serializable
private data class ModelsResponse(val models: List<KpiModelSummary>)

@Serializable
private data class CreateModelRequest(val name: String, val description: String, val inputs: KpiInputs)

@Serializable
private data class UpdateModelRequest(
    val name: String? = null,
    val description: String? = null,
    val inputs: KpiInputs? = null,
)

@Serializable
private data class ApiError(val error: String? = null)

internal class KtorKpiApi(private val client: HttpClient) : KpiApi {

    override suspend fun login(baseUrl: String, username: String, password: String): String {
        val response = client.post(baseUrl.api("/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        // Le JWT est posé en cookie `zesit_token` par le serveur (HttpOnly côté web) :
        // on le relit ici pour rejouer `Cookie: zesit_token=<jwt>` sur chaque appel.
        response.setCookie().firstOrNull { it.name == COOKIE }
            ?.let { return it.value }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw KpiAuthException("Identifiants KPI invalides.")
        }
        throw KpiApiException("Login KPI sans cookie (HTTP ${response.status.value}).")
    }

    override suspend fun me(baseUrl: String, token: String): String =
        decode(client.get(baseUrl.api("/me")) { auth(token) }, MeResponse.serializer()).user

    override suspend fun defaults(baseUrl: String, token: String): KpiInputs =
        decode(client.get(baseUrl.api("/defaults")) { auth(token) }, KpiInputs.serializer())

    override suspend fun model(baseUrl: String, token: String, inputs: KpiInputs): KpiResponse =
        decode(
            client.post(baseUrl.api("/model")) {
                auth(token)
                contentType(ContentType.Application.Json)
                setBody(inputs)
            },
            KpiResponse.serializer(),
        )

    override suspend fun models(baseUrl: String, token: String): List<KpiModelSummary> =
        decode(client.get(baseUrl.api("/models")) { auth(token) }, ModelsResponse.serializer()).models

    override suspend fun modelDetail(baseUrl: String, token: String, id: String): KpiModelDetail =
        decode(client.get(baseUrl.api("/models/$id")) { auth(token) }, KpiModelDetail.serializer())

    override suspend fun createModel(
        baseUrl: String,
        token: String,
        name: String,
        description: String,
        inputs: KpiInputs,
    ): KpiModelDetail = decode(
        client.post(baseUrl.api("/models")) {
            auth(token)
            contentType(ContentType.Application.Json)
            setBody(CreateModelRequest(name, description, inputs))
        },
        KpiModelDetail.serializer(),
    )

    override suspend fun updateModel(
        baseUrl: String,
        token: String,
        id: String,
        name: String?,
        description: String?,
        inputs: KpiInputs?,
    ): KpiModelDetail = decode(
        client.put(baseUrl.api("/models/$id")) {
            auth(token)
            contentType(ContentType.Application.Json)
            setBody(UpdateModelRequest(name, description, inputs))
        },
        KpiModelDetail.serializer(),
    )

    override suspend fun deleteModel(baseUrl: String, token: String, id: String) {
        ensureOk(client.delete(baseUrl.api("/models/$id")) { auth(token) }, "Suppression du modèle")
    }

    override suspend fun logout(baseUrl: String, token: String) {
        // Best effort : le serveur retire le cookie ; le token local est ensuite purgé.
        runCatching { client.post(baseUrl.api("/logout")) { auth(token) } }
    }

    // --- Helpers ---

    private fun io.ktor.client.request.HttpRequestBuilder.auth(token: String) {
        header("Cookie", "$COOKIE=$token")
    }

    private fun String.api(path: String): String = "${trimEnd('/')}/api$path"

    private suspend fun ensureOk(response: HttpResponse, what: String) {
        if (response.status == HttpStatusCode.Unauthorized) throw KpiAuthException()
        if (!response.status.isSuccess()) {
            throw KpiApiException("$what — HTTP ${response.status.value} : ${errorMessage(response)}")
        }
    }

    private suspend fun <T> decode(response: HttpResponse, serializer: DeserializationStrategy<T>): T {
        ensureOk(response, "Appel API KPI")
        return json.decodeFromString(serializer, response.bodyAsText())
    }

    private suspend fun errorMessage(response: HttpResponse): String =
        runCatching { json.decodeFromString(ApiError.serializer(), response.bodyAsText()).error }
            .getOrNull()
            ?: response.bodyAsText().take(200).ifBlank { "—" }

    private companion object {
        const val COOKIE = "zesit_token"
    }
}
