package fr.zesit.admin.di

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

// Keychain : réglages (avec le token ntfy) stockés comme mots de passe génériques.
internal actual fun createSecureSettings(): Settings =
    KeychainSettings(service = "fr.zesit.admin.settings")

internal actual fun createCacheSettings(): Settings = Settings()

internal actual fun createLegacySettings(): Settings? = Settings()
