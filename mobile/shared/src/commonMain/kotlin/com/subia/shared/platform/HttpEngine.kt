package com.subia.shared.platform

import io.ktor.client.engine.HttpClientEngine

/** Crea el motor HTTP nativo de la plataforma para Ktor. */
expect fun createHttpEngine(): HttpClientEngine
