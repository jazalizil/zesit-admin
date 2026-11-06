package fr.zesit.admin.apns

/**
 * Environnement APNs cible : "sandbox" en build Debug (certificats de dev),
 * "production" en Release/App Store. Le serveur ntfy doit utiliser la même
 * branche (clé .p8 + apns-topic du bundle id de l'app).
 */
expect fun apnsEnvironment(): String
