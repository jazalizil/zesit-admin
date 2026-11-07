package fr.zesit.admin

import android.app.Application
import fr.zesit.admin.di.AndroidAppContext

class ZesitAdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Requis par createSecureSettings() (EncryptedSharedPreferences) — voir
        // di/SettingsFactory.android.kt. Toujours appelé avant la première composition.
        AndroidAppContext.context = this
    }
}
