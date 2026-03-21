package com.subia.shared.di

import com.subia.shared.cache.CacheRepository
import com.subia.shared.network.ApiClient
import com.subia.shared.repository.AuthRepository
import com.subia.shared.repository.CatalogRepository
import com.subia.shared.repository.CategoryRepository
import com.subia.shared.repository.DashboardRepository
import com.subia.shared.repository.SubscriptionRepository
import com.subia.shared.viewmodel.AuthViewModel
import com.subia.shared.viewmodel.CatalogoViewModel
import com.subia.shared.viewmodel.CategoriasViewModel
import com.subia.shared.viewmodel.DashboardViewModel
import com.subia.shared.viewmodel.SuscripcionFormViewModel
import com.subia.shared.viewmodel.SuscripcionesViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Módulo Koin con todas las dependencias compartidas entre Android e iOS.
 * [apiBaseUrl] se inyecta desde BuildConfig en cada plataforma.
 * [isDebug] controla si el log HTTP está activo; debe ser `BuildConfig.DEBUG` de la plataforma.
 */
fun sharedModule(apiBaseUrl: String, isDebug: Boolean = false) = module {
    // Red
    single { ApiClient(baseUrl = apiBaseUrl, tokenStorage = get(), isDebug = isDebug) }

    // Caché persistente — Settings se provee desde androidModule (PlatformSettingsFactory)
    single { CacheRepository(get()) }

    // Repositorios
    single { AuthRepository(get(), get()) }
    single { DashboardRepository(get()) }
    single { SubscriptionRepository(get()) }
    single { CategoryRepository(get()) }
    single { CatalogRepository(get()) }

    // ViewModels
    viewModel { AuthViewModel(get()) }
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { SuscripcionesViewModel(get(), get(), get()) }
    viewModel { SuscripcionFormViewModel(get()) }
    viewModel { CategoriasViewModel(get()) }
    viewModel { CatalogoViewModel(get(), get()) }
}
