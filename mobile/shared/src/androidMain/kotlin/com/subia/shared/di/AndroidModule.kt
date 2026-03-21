package com.subia.shared.di

import com.russhwolf.settings.Settings
import com.subia.shared.cache.PlatformSettingsFactory
import com.subia.shared.platform.PlatformContext
import com.subia.shared.storage.TokenStorage
import com.subia.shared.storage.TokenStorageProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Módulo Koin con los enlaces específicos de Android.
 * Proporciona [PlatformContext], [TokenStorage] con EncryptedSharedPreferences,
 * y [Settings] respaldado por SharedPreferences("subia_cache") para el [com.subia.shared.cache.CacheRepository].
 *
 * [TokenStorage] se registra también como [TokenStorageProvider] para que [com.subia.shared.network.ApiClient]
 * pueda resolverlo independientemente de la plataforma.
 */
val androidModule = module {
    single { PlatformContext(androidContext()) }
    single { TokenStorage(get()) }
    // Registrar TokenStorage como TokenStorageProvider para que ApiClient pueda resolverlo por interfaz
    single<TokenStorageProvider> { get<TokenStorage>() }
    single<Settings> { PlatformSettingsFactory(androidContext()).create() }
}
