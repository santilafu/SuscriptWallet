package com.subia.shared.platform

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

/**
 * Motor `Android` (HttpURLConnection) y no `CIO`: CIO abre sus propios sockets y su propio TLS,
 * así que ignora la `NetworkSecurityPolicy` del sistema (network_security_config.xml, CAs del
 * sistema, proxies). Con CIO la app hablaba HTTP en claro aunque la configuración lo prohibía.
 */
actual fun createHttpEngine(): HttpClientEngine = Android.create()
