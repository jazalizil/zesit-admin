package fr.zesit.admin.di

/**
 * Env desktop : spécialise le commun avec la plateforme, l'OS/runtime hôte
 * et le mode debug explicite (ZESIT_DEBUG=1 au lancement).
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "desktop",
    debug = System.getenv("ZESIT_DEBUG") == "1",
    deviceInfo = "${System.getProperty("os.name")} · Java ${System.getProperty("java.version")}",
)
