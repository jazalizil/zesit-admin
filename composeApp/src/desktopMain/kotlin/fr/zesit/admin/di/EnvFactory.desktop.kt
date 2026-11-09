package fr.zesit.admin.di

/**
 * Env desktop : spécialise le commun avec la plateforme, l'OS/runtime hôte et
 * le mode debug explicite (ZESIT_DEBUG=1 au lancement). Le token TI vient de
 * la variable d'environnement `TI_API_TOKEN`.
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "desktop",
    debug = System.getenv("ZESIT_DEBUG") == "1",
    trafficToken = System.getenv("TI_API_TOKEN")?.takeIf { it.isNotBlank() },
    deviceInfo = "${System.getProperty("os.name")} · Java ${System.getProperty("java.version")}",
)
