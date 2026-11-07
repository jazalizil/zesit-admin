package fr.zesit.admin.di

import com.russhwolf.settings.Settings

// Pas de keystore standard sur JVM desktop : les réglages (token inclus)
// restent dans les Preferences Java en clair — pas de régression desktop,
// le token n'était pas chiffré avant non plus.
internal actual fun createSecureSettings(): Settings = Settings()

internal actual fun createCacheSettings(): Settings = Settings()

// Desktop : secure == legacy (même fichier Preferences Java), rien à migrer.
internal actual fun createLegacySettings(): Settings? = null
