import groovy.json.JsonSlurper
import java.io.File

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// ---------------------------------------------------------------------------
// Variables d'environnement — source unique : env.json (racine, non commitée ;
// modèle : env.json.example), sections { "common": {}, "android": {},
// "desktop": {}, "ios": {} } : les clés de « common » s'appliquent aux trois
// plateformes, surchargeables clé par clé dans chaque section ; valeur
// vide/absente = réglage par défaut du code.
//
// Table des rôles : une clé d'env × champ [AppEnv] qu'elle surcharge. Elle est
// la SOURCE DE VÉRITÉ de la consommation applicative — ./gradlew generateEnv
// en dérive les fichiers ci-dessous, les EnvFactory n'ont plus jamais à être
// édités à la main (ajouter une clé = l'ajouter ici + relancer generateEnv).
//
//   - composeApp/src/commonMain/kotlin/fr/zesit/admin/di/EnvOverrides.kt
//     (GÉNÉRÉ, commité) : withEnvOverrides(lookup) consomme toutes les clés
//     connues via la résolution fournie par chaque plateforme ;
//   - composeApp/src/androidMain/kotlin/fr/zesit/admin/di/AndroidEnv.kt
//     (GÉNÉRÉ, commité) : mapping clé → champ BuildConfig correspondant ;
//   - composeApp/env-android.properties (non commité) : valeur par clé pour
//     les buildConfigFields Android (voir composeApp/build.gradle.kts) ;
//   - desktop.env (racine) : lu au lancement par DesktopEnvFile (process
//     prioritaire sur le fichier) ;
//   - iosApp/iosApp/Env.local.xcconfig : chargé par Env.xcconfig (#include?) ;
//     les « // » sont échappés en « $(SLASH)/ » car « // » ouvre un commentaire
//     en xcconfig (sinon https://… serait tronqué en « https: »).
//
// Les clés d'env hors table (ex. NTFY_TOKEN) partent dans les fichiers
// desktop/ios mais ne sont consommées par aucun champ AppEnv.
// ---------------------------------------------------------------------------

// Triple(key d'env, champ AppEnv, kind: "token"|"url") — types stdlib
// uniquement pour rester sérialisable par le configuration cache.
val zesitEnvRoles: List<Triple<String, String, String>> = listOf(
    Triple("TI_API_TOKEN", "trafficToken", "token"),
    Triple("SC_API_TOKEN", "securityToken", "token"),
    Triple("NTFY_TOKEN", "ntfyToken", "token"),
    Triple("KPI_BASE_URL", "kpiBaseUrlDefault", "url"),
    Triple("NTFY_BASE_URL", "ntfyBaseUrlDefault", "url"),
    Triple("TRAFFIC_BASE_URL", "trafficBaseUrlDefault", "url"),
    Triple("SECURITY_BASE_URL", "securityBaseUrlDefault", "url"),
)

// Partagé au build.gradle.kts de composeApp (buildConfigFields Android).
extensions.extraProperties.set("zesitEnvKeys", zesitEnvRoles.map { it.first })

tasks.register("generateEnv") {
    group = "environment"
    description =
        "Génère desktop.env, env-android.properties, Env.local.xcconfig et les fichiers Env " +
            "consommateurs (EnvOverrides.kt, AndroidEnv.kt) depuis env.json (common/android/desktop/ios)."

    // Résolus à la configuration : le doLast ne capture que des valeurs
    // sérialisables (File, Triple…) — jamais le Project/script.
    val rootDir: File = projectDir
    val source = File(rootDir, "env.json")
    val roles: List<Triple<String, String, String>> = zesitEnvRoles
    val outputs: List<Pair<String, File>> = listOf(
        "desktop.env" to File(rootDir, "desktop.env"),
        "iosApp/iosApp/Env.local.xcconfig" to File(rootDir, "iosApp/iosApp/Env.local.xcconfig"),
        "composeApp/env-android.properties" to File(rootDir, "composeApp/env-android.properties"),
        "composeApp/src/commonMain/kotlin/fr/zesit/admin/di/EnvOverrides.kt" to
            File(rootDir, "composeApp/src/commonMain/kotlin/fr/zesit/admin/di/EnvOverrides.kt"),
        "composeApp/src/androidMain/kotlin/fr/zesit/admin/di/AndroidEnv.kt" to
            File(rootDir, "composeApp/src/androidMain/kotlin/fr/zesit/admin/di/AndroidEnv.kt"),
    )

    doLast {
        if (!source.isFile) {
            throw GradleException(
                "env.json introuvable : copier le modèle puis renseigner les valeurs " +
                    "(fichier ignoré par Git) : cp env.json.example env.json",
            )
        }
        val parsed: Map<*, *> = try {
            JsonSlurper().parse(source) as Map<*, *>
        } catch (e: Exception) {
            throw GradleException("env.json illisible : ${e.message}", e)
        }

        // Section en LinkedHashMap (ordre du JSON conservé), valeurs vides ignorées.
        fun section(name: String): LinkedHashMap<String, String> {
            val raw = parsed[name] as? Map<*, *> ?: emptyMap<Any?, Any?>()
            val result = LinkedHashMap<String, String>()
            raw.forEach { (key, value) ->
                val k = key?.toString()?.trim().orEmpty()
                val v = value?.toString().orEmpty()
                if (k.isNotEmpty() && v.isNotBlank()) result[k] = v
            }
            return result
        }

        val common = section("common")
        fun merged(platform: String): LinkedHashMap<String, String> =
            LinkedHashMap(common).apply { putAll(section(platform)) }

        fun dotenv(map: Map<String, String>, header: String): String = buildString {
            appendLine(header)
            map.forEach { (k, v) ->
                // Guillemets si la valeur contient un « # » (commentaire inline
                // du parseur desktop) ou une espace — DesktopEnvFile les retire.
                if (v.any { it == '#' || it == ' ' }) appendLine("$k=\"$v\"") else appendLine("$k=$v")
            }
        }

        fun xcconfig(map: Map<String, String>): String = buildString {
            appendLine("// Généré par ./gradlew generateEnv — NE PAS ÉDITER, source : env.json.")
            appendLine("// Chargé par Env.xcconfig (#include?) ; « // » y ouvre un commentaire,")
            appendLine("// d'où l'échappement des URLs via la variable SLASH.")
            map.forEach { (k, v) -> appendLine("$k = ${v.replace("//", "\$(SLASH)/")}") }
        }

        // Toutes les clés de la table des rôles, même absentes/vides de env.json :
        // la forme du fichier est stable, le BuildConfig Android suit toujours
        // l'intégralité de la table (clés présentes sur n'importe quelle plateforme).
        val androidValues: Map<String, String> = merged("android")
        fun androidProperties(): String = buildString {
            appendLine("# Généré par ./gradlew generateEnv — NE PAS ÉDITER, source : env.json.")
            appendLine("# Toutes les clés de la table des rôles (build.gradle.kts racine) :")
            appendLine("# priorité propriété Gradle > variable d'environnement > ce fichier.")
            roles.forEach { (key, _, _) -> appendLine("$key=${androidValues[key].orEmpty()}") }
        }

        fun envOverridesKt(): String = buildString {
            appendLine("package fr.zesit.admin.di")
            appendLine()
            appendLine("/**")
            appendLine(" * GÉNÉRÉ par ./gradlew generateEnv — NE PAS ÉDITER (source :")
            appendLine(" * table des rôles du build.gradle.kts à la racine).")
            appendLine(" *")
            appendLine(" * Consommation des variables d'env dans [AppEnv] : chaque plateforme")
            appendLine(" * fournit la résolution via [lookup] (process prioritaire, puis fichier")
            appendLine(" * desktop.env / Info.plist / BuildConfig selon la cible) ; une valeur")
            appendLine(" * vide ou hors contrat est ignorée → réglage par défaut du code.")
            appendLine(" */")
            appendLine("internal fun AppEnv.withEnvOverrides(lookup: (key: String) -> String?): AppEnv {")
            appendLine("    var env = this")
            appendLine("    // Jetons.")
            roles.filter { it.third == "token" }.forEach { (key, field, _) ->
                appendLine("    lookup(\"$key\")?.takeIf(String::isNotBlank)?.let { env = env.copy($field = it) }")
            }
            appendLine("    // URLs par défaut (garde-fou : http(s):// complet).")
            roles.filter { it.third == "url" }.forEach { (key, field, _) ->
                appendLine("    lookup(\"$key\")?.let { envBaseUrl(it)?.let { url -> env = env.copy($field = url) } }")
            }
            appendLine("    return env")
            appendLine("}")
        }

        fun androidEnvKt(): String = buildString {
            appendLine("package fr.zesit.admin.di")
            appendLine()
            appendLine("import fr.zesit.admin.BuildConfig")
            appendLine()
            appendLine("/**")
            appendLine(" * GÉNÉRÉ par ./gradlew generateEnv — NE PAS ÉDITER (source :")
            appendLine(" * table des rôles du build.gradle.kts à la racine).")
            appendLine(" *")
            appendLine(" * Résolution Android des variables d'env depuis les champs BuildConfig")
            appendLine(" * générés par composeApp/build.gradle.kts (propriété Gradle > variable")
            appendLine(" * d'environnement > env-android.properties). Vide → défaut du code.")
            appendLine(" */")
            appendLine("internal fun androidEnvLookup(key: String): String? =")
            appendLine("    when (key) {")
            roles.forEach { (key, _, _) -> appendLine("        \"$key\" -> BuildConfig.$key") }
            appendLine("        else -> null")
            appendLine("    }?.takeUnless(String::isBlank)")
        }

        fun writeIfChanged(label: String, target: File, content: String) {
            if (target.isFile && target.readText() == content) {
                logger.lifecycle("inchangé : $label")
                return
            }
            target.parentFile?.mkdirs()
            target.writeText(content)
            logger.lifecycle("généré : $label")
        }

        writeIfChanged(
            outputs[0].first,
            outputs[0].second,
            dotenv(merged("desktop"), "# Généré par ./gradlew generateEnv — NE PAS ÉDITER, source : env.json."),
        )
        writeIfChanged(outputs[1].first, outputs[1].second, xcconfig(merged("ios")))
        writeIfChanged(outputs[2].first, outputs[2].second, androidProperties())
        writeIfChanged(outputs[3].first, outputs[3].second, envOverridesKt())
        writeIfChanged(outputs[4].first, outputs[4].second, androidEnvKt())
    }
}
