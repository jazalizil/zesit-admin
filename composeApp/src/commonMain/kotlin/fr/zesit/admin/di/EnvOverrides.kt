package fr.zesit.admin.di

/**
 * GÉNÉRÉ par ./gradlew generateEnv — NE PAS ÉDITER (source :
 * table des rôles du build.gradle.kts à la racine).
 *
 * Consommation des variables d'env dans [AppEnv] : chaque plateforme
 * fournit la résolution via [lookup] (process prioritaire, puis fichier
 * desktop.env / Info.plist / BuildConfig selon la cible) ; une valeur
 * vide ou hors contrat est ignorée → réglage par défaut du code.
 */
internal fun AppEnv.withEnvOverrides(lookup: (key: String) -> String?): AppEnv {
    var env = this
    // Jetons.
    lookup("TI_API_TOKEN")?.takeIf(String::isNotBlank)?.let { env = env.copy(trafficToken = it) }
    lookup("SC_API_TOKEN")?.takeIf(String::isNotBlank)?.let { env = env.copy(securityToken = it) }
    lookup("NTFY_TOKEN")?.takeIf(String::isNotBlank)?.let { env = env.copy(ntfyToken = it) }
    // URLs par défaut (garde-fou : http(s):// complet).
    lookup("KPI_BASE_URL")?.let { envBaseUrl(it)?.let { url -> env = env.copy(kpiBaseUrlDefault = url) } }
    lookup("NTFY_BASE_URL")?.let { envBaseUrl(it)?.let { url -> env = env.copy(ntfyBaseUrlDefault = url) } }
    lookup("TRAFFIC_BASE_URL")?.let { envBaseUrl(it)?.let { url -> env = env.copy(trafficBaseUrlDefault = url) } }
    lookup("SECURITY_BASE_URL")?.let { envBaseUrl(it)?.let { url -> env = env.copy(securityBaseUrlDefault = url) } }
    return env
}
