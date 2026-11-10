package fr.zesit.admin.di

import java.io.File

/**
 * Variables d'environnement desktop : l'env du process (export shell, run
 * config IDEA) **prime**, complété var par var par un fichier dotenv
 * `desktop.env` — le seul canal qui marche aussi pour l'app packagée
 * (.app/.dmg lancée depuis Finder, qui n'hérite pas des exports du shell).
 *
 * Recherche du fichier, par ordre (le premier trouvé est le seul lu) :
 * 1. chemin de la variable d'environnement [OVERRIDE_ENV_VAR] ;
 * 2. `desktop.env` dans le dossier de travail (racine du projet en dev —
 *    `./gradlew :composeApp:run` comme depuis IDEA) ;
 * 3. `~/.zesit-admin/desktop.env` (emplacement stable pour l'app packagée).
 *
 * Format dotenv : une `CLE=valeur` par ligne, `#` commente, guillemets
 * facultatifs autour de la valeur.
 */
internal object DesktopEnvFile {

    /** Chemin explicite d'un dotenv de substitution (tests, config exotique). */
    private const val OVERRIDE_ENV_VAR = "ZESIT_DESKTOP_ENV"
    private const val FILE_NAME = "desktop.env"
    private const val HOME_DIR = ".zesit-admin"

    /** Valeur effective d'une variable : process → fichier, vide ignorée. */
    fun value(name: String): String? =
        System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: values[name]?.takeIf { it.isNotBlank() }

    private val values: Map<String, String> by lazy { fileValues() }

    private fun fileValues(): Map<String, String> {
        val file = firstExisting(candidates()) ?: return emptyMap()
        return try {
            parse(file.readText())
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /** Chemins candidats dans l'ordre de priorité de recherche. */
    private fun candidates(): List<File> = listOfNotNull(
        System.getenv(OVERRIDE_ENV_VAR)?.takeIf { it.isNotBlank() }?.let(::File),
    ) + walkUpFromWorkingDir() + listOf(
        File(System.getProperty("user.home"), "$HOME_DIR/$FILE_NAME"),
    )

    /**
     * `desktop.env` dans le dossier de travail puis en remontant jusqu'à la
     * racine du dépôt : lancée par `./gradlew :composeApp:run`, l'app démarre
     * avec user.dir = composeApp/ alors que le dotenv généré vit à la racine
     * du projet — sans cette remontée, aucune variable (SC_API_TOKEN,
     * SECURITY_BASE_URL, …) n'était résolue depuis Gradle/IDEA.
     */
    private fun walkUpFromWorkingDir(): Sequence<File> {
        val start = File(System.getProperty("user.dir") ?: ".")
        return generateSequence(start) { it.parentFile }
            .take(4) // composeApp/ → racine du dépôt (marge pour les sur-répertoires)
            .map { File(it, FILE_NAME) }
    }

    /** Premier fichier existant de la liste, sinon null. */
    internal fun firstExisting(candidates: List<File>): File? =
        candidates.firstOrNull { it.isFile }

    /** Analyse dotenv : `CLE=valeur`, `#` commentaires, guillemets facultatifs. */
    internal fun parse(text: String): Map<String, String> =
        text.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val sep = line.indexOfFirst { it == '=' }
                if (sep <= 0) return@mapNotNull null
                val key = line.substring(0, sep).trim()
                val raw = line.substring(sep + 1).trim()
                // Guillemets appariés → valeur telle quelle ; sinon « # »
                // en fin de ligne démarre un commentaire.
                val value = if (raw.length >= 2 && (raw.first() == '"' || raw.first() == '\'') && raw.last() == raw.first()) {
                    raw.substring(1, raw.length - 1)
                } else {
                    raw.substringBefore(" #").trim()
                }
                key to value
            }
            .toMap()
}
