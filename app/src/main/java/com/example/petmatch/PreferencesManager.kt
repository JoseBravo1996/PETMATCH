package com.example.petmatch

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class PreferencesManager(val context:Context) {
    val KEY_LANGUAGE = "pref_language"
    val storage = context.getSharedPreferences(KEY_LANGUAGE, 0)
}

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
}