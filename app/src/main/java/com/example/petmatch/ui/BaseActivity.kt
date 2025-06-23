package com.example.petmatch.ui

import android.content.Context
import android.media.MediaFormat.KEY_LANGUAGE
import androidx.appcompat.app.AppCompatActivity
import com.example.petmatch.LocaleHelper
import java.util.Locale

abstract class BaseActivity: AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("petmatch_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language)!!
        val ctx = LocaleHelper.setLocale(newBase, lang)
        super.attachBaseContext(ctx)
    }
}