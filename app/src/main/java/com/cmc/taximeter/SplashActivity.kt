package com.cmc.taximeter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L // 2 secondes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Vérifier si l'utilisateur est déjà connecté
        Handler(Looper.getMainLooper()).postDelayed({
            checkLoginStatus()
        }, SPLASH_DELAY)
    }

    private fun checkLoginStatus() {
        val sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val loggedInUser = sharedPreferences.getString("loggedInUser", null)

        val intent = if (loggedInUser != null) {
            // Utilisateur déjà connecté, aller directement à MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // Utilisateur non connecté, aller à AuthenticationActivity
            Intent(this, AuthenticationActivity::class.java)
        }

        startActivity(intent)
        finish()
    }
}