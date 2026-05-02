package com.exchenged.client.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    fun wrapContext(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val config = Configuration(resources.configuration)
        
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }

    fun applyLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Also update application context for Toasts and other system-level calls
        val appContext = context.applicationContext
        val appConfig = appContext.resources.configuration
        appConfig.setLocale(locale)
        appContext.resources.updateConfiguration(appConfig, appContext.resources.displayMetrics)
    }
}
