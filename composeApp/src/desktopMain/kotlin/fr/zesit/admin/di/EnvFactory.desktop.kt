package fr.zesit.admin.di

/**
 * Env desktop : spécialise le commun avec la plateforme, l'OS/runtime hôte et
 * le mode debug explicite (ZESIT_DEBUG=1).
 *
 * Les autres variables d'env ([withEnvOverrides], généré par
 * ./gradlew generateEnv depuis la table des rôles) sont résolues par
 * [DesktopEnvFile] : environnement du process prioritaire, puis fichier
 * `desktop.env` (racine ou `~/.zesit-admin/`).
 */
actual fun platformEnv(): AppEnv = commonEnv().copy(
    platform = "desktop",
    debug = DesktopEnvFile.value("ZESIT_DEBUG") == "1",
    deviceInfo = "${System.getProperty("os.name")} · Java ${System.getProperty("java.version")}",
).withEnvOverrides(DesktopEnvFile::value)
