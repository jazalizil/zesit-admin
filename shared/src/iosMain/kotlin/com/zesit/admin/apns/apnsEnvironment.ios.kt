package com.zesit.admin.apns

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
actual fun apnsEnvironment(): String =
    if (Platform.isDebugBinary) "sandbox" else "production"
