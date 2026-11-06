package com.zesit.admin.ntfy

import io.ktor.client.HttpClient

/** Moteur Ktor par plateforme (OkHttp sur Android, Darwin sur iOS). */
expect fun createHttpClient(): HttpClient
