package com.example.petmatch.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.petmatch.LocaleHelper
import java.util.Locale

abstract class BaseActivity: AppCompatActivity() {
    companion object {
        private const val KEY_LANGUAGE = "pref_language"
    }
    
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("petmatch_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language)!!
        val ctx = LocaleHelper.setLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }
}