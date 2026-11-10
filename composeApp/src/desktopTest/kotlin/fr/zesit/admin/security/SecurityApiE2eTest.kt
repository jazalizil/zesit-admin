package fr.zesit.admin.security

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * E2E du client /sc-api contre un vrai security-scanner (zesit-vps), activé
 * uniquement si les variables d'environnement pointent dessus :
 *   SC_E2E_URL=http://127.0.0.1:8124 SC_E2E_TOKEN=… ./gradlew :composeApp:desktopTest
 * (docker run --rm -p 127.0.0.1:8124:8000 -v zesit-vps/data/security-scanner:/data \
 *   -e SC_API_TOKEN=… -e SC_DISABLE_SCHEDULER=1 security-scanner:latest)
 *
 * Sans ces variables, les tests passent à vide (aucun appel réseau).
 *
 * Avec SC_DISABLE_SCHEDULER=1 côté serveur, un run 'requested' n'est jamais
 * ramassé : le POST /sc-api/scan crée le run (202) puis répond 409 tant qu'un
 * run est actif — le test accepte les deux ordres d'arrivée.
 */
class SecurityApiE2eTest {

    private val url: String? = System.getenv("SC_E2E_URL")
    private val token: String? = System.getenv("SC_E2E_TOKEN")

    @Test
    fun health_summary_findings_scans_et_scan_manuel() {
        if (url == null || token == null) return // E2E non configuré : à vide.
        val api = createSecurityApi()
        runBlocking {
            // health (public)
            val health = api.health(url)
            assertTrue(health.ok, "health.ok attendu : $health")
            assertTrue(health.api_enabled, "SC_API_TOKEN doit être actif côté serveur")
            val trivyVersion = health.trivy_version
            assertTrue(
                trivyVersion == null || trivyVersion.contains("Version") || trivyVersion.contains("."),
                "version Trivy lisible : $trivyVersion",
            )

            // summary (Bearer)
            val summary = api.summary(url, token)
            assertTrue(summary.images.isEmpty() || summary.images.all { it.image_id.startsWith("sha256:") })
            // La base de test contient au moins une image déjà scannée (cycles locaux).
            assertTrue(
                summary.images.isNotEmpty(),
                "E2E : la base du scanner local doit contenir au moins une image scannée",
            )
            val img = summary.images.first()
            assertTrue(img.name.isNotBlank())

            // findings de la 1re image (Bearer + filtres)
            val detail = api.findings(url, token, img.image_id, limit = 10)
            assertTrue(detail.total >= detail.findings.size, "total >= page renvoyée")
            detail.findings.forEach { f ->
                assertTrue(f.cve_id.isNotEmpty(), "cve_id non vide : $f")
                assertTrue(f.severity.isNotBlank())
            }

            // historique
            val scans = api.scans(url, token, limit = 5)
            assertTrue(scans.runs.first().id > 0)

            // scan manuel : 202 -> run créé, puis 409 (déjà en attente). Avec
            // SC_DISABLE_SCHEDULER=1, un run 'requested' laissé par un E2E
            // précédent peut répondre 409 dès le premier appel : les deux
            // issues sont valides (serveur vivant = un run actif max).
            val first = runCatching { api.triggerScan(url, token) }
            if (first.isSuccess) {
                assertTrue(first.getOrDefault(0) > 0)
                assertFailsWith<SecurityScanConflictException> { api.triggerScan(url, token) }
            } else {
                assertTrue(
                    first.exceptionOrNull() is SecurityScanConflictException,
                    "le 1er POST ne doit échouer que par conflit 409 : ${first.exceptionOrNull()}",
                )
            }
        }
    }

    @Test
    fun mauvais_token_refuse() {
        if (url == null || token == null) return
        val api = createSecurityApi()
        runBlocking {
            try {
                api.summary(url, "mauvais-token")
                fail("un mauvais token doit être refusé (401)")
            } catch (e: SecurityAuthException) {
                // attendu
            }
        }
    }
}
