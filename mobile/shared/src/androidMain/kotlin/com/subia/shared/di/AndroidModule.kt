package com.subia.shared.di

import com.subia.shared.platform.PlatformContext
import com.subia.shared.storage.TokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Módulo Koin con los enlaces específicos de Android.
 * Proporciona [PlatformContext] y [TokenStorage] con EncryptedSharedPreferences.
 */
val androidModule = module {
    single { PlatformContext(androidContext()) }
    single { TokenStorage(get()) }
}
