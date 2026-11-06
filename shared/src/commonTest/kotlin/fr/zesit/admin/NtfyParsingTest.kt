package fr.zesit.admin

import fr.zesit.admin.model.NtfyMessage
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NtfyParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseNotificationLine() {
        // Ligne type renvoyée par GET /<topic>/json?poll=1 (NDJSON) et par le WebSocket
        val line = """{"id":"dzJJm7BCWs","time":1637182634,"event":"message","topic":"zesit-abc","title":"Backup zesit","message":"✅ 3 projets","priority":4,"tags":["bell","check"]}"""
        val msg = json.decodeFromString<NtfyMessage>(line)
        assertTrue(msg.isNotification)
        assertEquals("Backup zesit", msg.title)
        assertEquals(4, msg.priority)
        assertEquals(listOf("bell", "check"), msg.tags)
        val notif = msg.toNotification()
        assertEquals("dzJJm7BCWs", notif.id)
        assertEquals("Backup zesit", notif.title)
    }

    @Test
    fun keepaliveIsNotANotification() {
        val keepalive = json.decodeFromString<NtfyMessage>("""{"id":"x","time":1,"event":"keepalive"}""")
        assertFalse(keepalive.isNotification)
    }

    @Test
    fun missingTitleFallsBack() {
        val msg = json.decodeFromString<NtfyMessage>("""{"id":"y","time":2,"message":"hello"}""")
        assertEquals("Notification", msg.toNotification().title)
    }
}
