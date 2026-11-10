package fr.zesit.admin.di

import fr.zesit.admin.BuildConfig

/**
 * GÉNÉRÉ par ./gradlew generateEnv — NE PAS ÉDITER (source :
 * table des rôles du build.gradle.kts à la racine).
 *
 * Résolution Android des variables d'env depuis les champs BuildConfig
 * générés par composeApp/build.gradle.kts (propriété Gradle > variable
 * d'environnement > env-android.properties). Vide → défaut du code.
 */
internal fun androidEnvLookup(key: String): String? =
    when (key) {
        "TI_API_TOKEN" -> BuildConfig.TI_API_TOKEN
        "SC_API_TOKEN" -> BuildConfig.SC_API_TOKEN
        "NTFY_TOKEN" -> BuildConfig.NTFY_TOKEN
        "KPI_BASE_URL" -> BuildConfig.KPI_BASE_URL
        "NTFY_BASE_URL" -> BuildConfig.NTFY_BASE_URL
        "TRAFFIC_BASE_URL" -> BuildConfig.TRAFFIC_BASE_URL
        "SECURITY_BASE_URL" -> BuildConfig.SECURITY_BASE_URL
        else -> null
    }?.takeUnless(String::isBlank)
