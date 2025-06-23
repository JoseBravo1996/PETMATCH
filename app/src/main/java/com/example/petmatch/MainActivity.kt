package com.example.petmatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.petmatch.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.petmatch.databinding.ActivityMainBinding
import com.example.petmatch.ui.BaseActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Esto "engancha" tu toolbar como ActionBar
        setSupportActionBar(binding.toolbar)

        // Recupera tu NavController
        val navHost = supportFragmentManager
            .findFragmentById(R.id.mainNavHost) as NavHostFragment
        navController = navHost.navController

        // BottomNavigationView setup
        val bottomNav = binding.bottomNavigationView
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> {
                    // TODO: Implementar DiscoverFragment
                    true
                }
                R.id.nav_swipe -> {
                    navController.navigate(R.id.swipeFragment)
                    true
                }
                R.id.nav_likes -> {
                    navController.navigate(R.id.matchesFragment)
                    true
                }
                R.id.nav_chats -> {
                    // TODO: Implementar ChatsFragment
                    true
                }
                R.id.nav_profile -> {
                    navController.navigate(R.id.addPetFragment)
                    true
                }
                else -> false
            }
        }

        // Listener explícito de clicks de menú
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_matches -> {
                    navController.navigate(R.id.matchesFragment)
                    true
                }
                R.id.action_sign_out -> {
                    auth.signOut()
                    startActivity(Intent(this, AuthActivity::class.java))
                    finish()
                    true
                }
                R.id.action_settings -> {
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                else -> false
            }
        }

        // El FAB para agregar mascotas
        //binding.fabAddPet.setOnClickListener {
        //    navController.navigate(R.id.addPetFragment)
        //}

        createNotificationChannel()
        createDownloadChannel()
    }

    // Infla el menú de cerrar sesión
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    // Maneja el clic en "Cerrar sesión"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Matches Mutuos"
            val descriptionText = "Notificaciones de nuevos matches mutuos"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(
                "match_channel",
                name,
                importance
            ).apply {
                description = descriptionText
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun createDownloadChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Descarga de Fotos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones cuando termine la descarga de fotos"
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }
}