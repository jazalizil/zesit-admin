package fr.zesit.admin

import fr.zesit.admin.ntfy.KtorNtfyApi
import fr.zesit.admin.ntfy.NtfyAuthException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NtfyApiTest {

    private fun api(handler: MockRequestHandler): KtorNtfyApi {
        val client = HttpClient(MockEngine(handler)) {
            install(ContentNegotiation) { json() }
        }
        return KtorNtfyApi(client)
    }

    private fun requestBody(req: HttpRequestData): String = when (val b = req.body) {
        is String -> b
        is OutgoingContent.ByteArrayContent -> b.bytes().decodeToString()
        else -> b.toString()
    }

    @Test
    fun pollParsesNdjsonAndSendsBearer() = runTest {
        var seenAuth: String? = null
        val api = api { req ->
            seenAuth = req.headers[HttpHeaders.Authorization]
            respond(
                content = """
                    {"id":"a1","time":100,"event":"message","topic":"t","title":"T1","message":"m1","priority":3}
                    {"id":"k1","time":101,"event":"keepalive"}
                    {"id":"a2","time":102,"event":"message","topic":"t","message":"m2","priority":5}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }
        val messages = api.poll("https://ntfy.zesit.fr", "zesit-topic", "tk_secret", sinceId = "a0")
        assertEquals(2, messages.size) // keepalive filtré
        assertEquals(listOf("a1", "a2"), messages.map { it.id })
        assertEquals("Bearer tk_secret", seenAuth)
        api.close()
    }

    @Test
    fun poll401ThrowsAuthException() = runTest {
        val api = api { respond(content = "", status = HttpStatusCode.Unauthorized) }
        assertFailsWith<NtfyAuthException> {
            api.poll("https://ntfy.zesit.fr", "t", "tk_bad")
        }
        api.close()
    }

    @Test
    fun registerApnsDevicePostsToV1Apns() = runTest {
        var body = ""
        var auth: String? = null
        var path = ""
        val api = api { req ->
            auth = req.headers[HttpHeaders.Authorization]
            path = req.url.encodedPath
            body = requestBody(req)
            respond(content = """{"id":"ok"}""", status = HttpStatusCode.OK)
        }
        api.registerApnsDevice("https://ntfy.zesit.fr", "tk_secret", "ABCDEF0123", "production", listOf("zesit-topic"))
        assertEquals("/v1/apns", path)
        assertTrue(body.contains("\"token\":\"abcdef0123\""), body) // lowercased
        assertTrue(body.contains("\"environment\":\"production\""), body)
        assertTrue(body.contains("\"topics\":[\"zesit-topic\"]"), body)
        assertEquals("Bearer tk_secret", auth)
        api.close()
    }

    @Test
    fun checkAccessTrueOn200() = runTest {
        val api = api { respond(content = "", status = HttpStatusCode.OK) }
        assertTrue(api.checkAccess("https://ntfy.zesit.fr", "t", null))
        api.close()
    }
}
