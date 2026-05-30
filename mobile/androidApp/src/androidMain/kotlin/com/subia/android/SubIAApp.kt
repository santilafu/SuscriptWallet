package com.subia.android

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.subia.android.BuildConfig
import com.subia.shared.di.androidModule
import com.subia.shared.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/** Application principal de SubIA. Inicializa Koin con los módulos compartidos y Android. */
class SubIAApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // MobileAds es prescindible: si su init falla (Play Services, ID inválido), la app
        // debe seguir arrancando sin anuncios en lugar de crashear.
        try {
            MobileAds.initialize(this) {}
        } catch (e: Throwable) {
            Log.w("SubIAApp", "Fallo al inicializar MobileAds; se continúa sin anuncios", e)
        }
        startKoin {
            androidContext(this@SubIAApp)
            modules(
                androidModule,
                sharedModule(apiBaseUrl = BuildConfig.API_BASE_URL, isDebug = BuildConfig.DEBUG)
            )
        }
    }
}
