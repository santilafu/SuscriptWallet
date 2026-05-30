package com.subia.android.util

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/** Abre una URL en una pestaña de Chrome Custom Tabs (mejor UX que el navegador externo). */
fun openCustomTab(context: Context, url: String) {
    CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
        .launchUrl(context, url.toUri())
}
