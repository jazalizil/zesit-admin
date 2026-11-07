package fr.zesit.admin.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/** Posé par `ZesitAdminApplication.onCreate` au démarrage du process. */
internal object AndroidAppContext {
    @Volatile var context: Context? = null
}

internal actual fun createSecureSettings(): Settings {
    val context = AndroidAppContext.context
        ?: error("AndroidAppContext non initialisé — ZesitAdminApplication.onCreate doit avoir été appelé")
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    return SharedPreferencesSettings(
        EncryptedSharedPreferences.create(
            context,
            "zesit_admin_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    )
}

internal actual fun createCacheSettings(): Settings = Settings()

internal actual fun createLegacySettings(): Settings? = Settings()
