package com.example.petmatch.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.petmatch.MainActivity
import com.example.petmatch.R
import com.example.petmatch.ui.BaseActivity

class AuthActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_auth.xml contiene el NavHostFragment para login/register
        setContentView(R.layout.activity_auth)

        // Si ya hay usuario logueado, vamos directo a MainActivity
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}