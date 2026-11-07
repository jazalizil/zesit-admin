package fr.zesit.admin.apns

// Pas d'APNs sur desktop : valeur neutre (jamais utilisée, le bridge APNs est iOS-only).
actual fun apnsEnvironment(): String = "production"
