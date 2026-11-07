package fr.zesit.admin.di

import com.russhwolf.settings.Settings

/**
 * Stockage sécurisé des [fr.zesit.admin.data.AppSettings] (qui contiennent le
 * token ntfy) : Keychain sur iOS, EncryptedSharedPreferences sur Android.
 * Sur desktop (JVM), pas de keystore standard : Preferences Java (comme avant).
 */
internal expect fun createSecureSettings(): Settings

/** Stockage standard du cache de notifications (aucun secret dedans). */
internal expect fun createCacheSettings(): Settings

/**
 * Ancien stockage des réglages (avant chiffrement) : default SharedPreferences
 * (Android) / NSUserDefaults (iOS), pour migration unique. Null sur desktop —
 * le stockage « sécurisé » y est le même fichier Preferences Java, rien à migrer.
 */
internal expect fun createLegacySettings(): Settings?
