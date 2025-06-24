package com.example.petmatch.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.petmatch.LocaleHelper
import com.example.petmatch.MainActivity
import com.example.petmatch.R
import java.util.*

private const val PREFS_NAME = "petmatch_prefs"
private const val KEY_LANGUAGE = "pref_language"

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val languages = listOf(
        "es" to "Español",
        "en" to "English",
        "pt" to "Português"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1) Referencia al Spinner
        val spinner = view.findViewById<Spinner>(R.id.spinnerLanguage)

        // 2) Cargar preferencias
        val prefs = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLang = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language)!!

        // 3) Adapter
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            languages.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // 4) Establecer la selección SIN disparar listener
        val initialPosition = languages.indexOfFirst { it.first == currentLang }
            .coerceAtLeast(0)
        spinner.setSelection(initialPosition, /* animate = */ false)

        // 5) Ahora sí, asignamos el listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            var firstCall = true
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Ignoramos la primera llamada que viene de setSelection()
                if (firstCall) {
                    firstCall = false
                    return
                }

                // Solo si cambió realmente
                val selectedCode = languages[position].first
                if (selectedCode != currentLang) {
                    // 1) Guardar en prefs
                    prefs.edit().putString(KEY_LANGUAGE, selectedCode).apply()
                    
                    // 2) Mostrar mensaje
                    Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
                    
                    // 3) Reiniciar la app completamente
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    requireActivity().finish()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }
}