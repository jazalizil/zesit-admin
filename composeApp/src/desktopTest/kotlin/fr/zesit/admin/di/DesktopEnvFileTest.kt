package fr.zesit.admin.di

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopEnvFileTest {

    // --- parse ---

    @Test
    fun parse_lit_les_paires_cle_valeur_espaces_et_commentaires_inclus() {
        val values = DesktopEnvFile.parse(
            """
            # commentaire en tête
            TI_API_TOKEN=abc-def_123

              NTFY_BASE_URL = https://ntfy.zesit.fr
            # ZESIT_DEBUG=1 (commenté = ignoré)
            TRAFFIC_BASE_URL=https://traffic.zesit.fr  # url avec commentaire en fin de ligne
            LIGNE_SANS_EGAL
            =VALEUR_SANS_CLE
            """.trimIndent(),
        )
        assertEquals("abc-def_123", values["TI_API_TOKEN"])
        assertEquals("https://ntfy.zesit.fr", values["NTFY_BASE_URL"])
        assertEquals("https://traffic.zesit.fr", values["TRAFFIC_BASE_URL"])
        assertTrue("ZESIT_DEBUG" !in values, "une ligne commentée doit être ignorée")
        assertEquals(3, values.size)
    }

    @Test
    fun parse_retire_les_guillemets_apparies_seulement() {
        val values = DesktopEnvFile.parse(
            """
            A="valeur entre guillemets"
            B='entre simples'
            C="non apparié'
            D=
            E="avec # dièse à l'intérieur"
            """.trimIndent(),
        )
        assertEquals("valeur entre guillemets", values["A"])
        assertEquals("entre simples", values["B"])
        assertEquals("\"non apparié'", values["C"])
        assertEquals("", values["D"], "valeur vide conservée (clé présente)")
        assertEquals("avec # dièse à l'intérieur", values["E"], "guillemets = pas de commentaire inline")
    }

    @Test
    fun parse_d_un_fichier_vide_ou_commenté_ne_rend_rien() {
        assertTrue(DesktopEnvFile.parse("").isEmpty())
        assertTrue(DesktopEnvFile.parse("# rien\n\n   \n").isEmpty())
    }

    // --- firstExisting (ordre de recherche) ---

    @Test
    fun firstExisting_rend_le_premier_fichier_existant_de_l_ordre_de_recherche() {
        val root = Files.createTempDirectory("zesit-env-test").toFile()
        try {
            val home = root.resolve("home").apply { mkdirs() }
            val cwd = root.resolve("cwd").apply { mkdirs() }

            // Aucun fichier → null même si le dossier existe.
            assertTrue(
                DesktopEnvFile.firstExisting(
                    listOf(cwd.resolve("desktop.env"), home.resolve("desktop.env")),
                ) == null,
            )

            // Fichier home seul → trouvé.
            val homeEnv = home.resolve("desktop.env").apply { writeText("A=1\n") }
            assertEquals(
                homeEnv,
                DesktopEnvFile.firstExisting(listOf(cwd.resolve("desktop.env"), homeEnv)),
            )

            // Dossier de travail prioritaire sur home.
            val cwdEnv = cwd.resolve("desktop.env").apply { writeText("A=2\n") }
            assertEquals(
                cwdEnv,
                DesktopEnvFile.firstExisting(listOf(cwdEnv, homeEnv)),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    // --- envBaseUrl (EnvFactory) ---

    @Test
    fun envBaseUrl_n_accepte_qu_une_url_http_complete() {
        assertEquals("https://x.zesit.fr", envBaseUrl("https://x.zesit.fr"))
        assertEquals("http://localhost:8080", envBaseUrl("  http://localhost:8080  "))
        // Vide, tronquée, littéral $(VAR) non résolu, hors http(s) → ignorés.
        assertTrue(envBaseUrl(null) == null)
        assertTrue(envBaseUrl("") == null)
        assertTrue(envBaseUrl("   ") == null)
        assertTrue(envBaseUrl("https:") == null)
        assertTrue(envBaseUrl("$(TRAFFIC_BASE_URL)") == null)
        assertTrue(envBaseUrl("kpi.zesit.fr") == null)
    }
}
