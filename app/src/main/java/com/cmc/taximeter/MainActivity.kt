package com.cmc.taximeter

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnDarkMode: MaterialButton
    private lateinit var btnLanguage: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème et la langue AVANT super.onCreate()
        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        applyDarkMode()
        applyLanguage()

        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            enableEdgeToEdge()
        }
        setContentView(R.layout.activity_main)

        initializeViews()
        setupViewPager()
        setupButtons()
    }

    private fun initializeViews() {
        try {
            viewPager = findViewById(R.id.viewPager)
            tabLayout = findViewById(R.id.tabLayout)
            btnDarkMode = findViewById(R.id.btnDarkMode)
            btnLanguage = findViewById(R.id.btnLanguage)
            btnLogout = findViewById(R.id.btnLogout)
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur d'initialisation: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun setupViewPager() {
        val userEmail = sharedPreferences.getString("loggedInUser", "email@exemple.com") ?: "email@exemple.com"
        viewPager.adapter = ViewPagerAdapter(this, userEmail)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.setIcon(R.drawable.ic_speedometer)
                    tab.text = getString(R.string.tab_counter)
                }
                1 -> {
                    tab.setIcon(R.drawable.ic_map_marker)
                    tab.text = getString(R.string.tab_map)
                }
                2 -> {
                    tab.setIcon(R.drawable.ic_user_circle)
                    tab.text = getString(R.string.tab_profile)
                }
            }
        }.attach()
    }

    private fun setupButtons() {
        // Bouton Dark Mode
        updateDarkModeIcon()
        btnDarkMode.setOnClickListener {
            toggleDarkMode()
        }

        // Bouton Language - NE PAS RECRÉER L'ACTIVITÉ
        btnLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Bouton Déconnexion
        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun applyDarkMode() {
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun toggleDarkMode() {
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        val newMode = !isDarkMode

        sharedPreferences.edit().putBoolean("isDarkMode", newMode).apply()

        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        updateDarkModeIcon()
    }

    private fun updateDarkModeIcon() {
        val isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        val iconRes = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon
        btnDarkMode.setIconResource(iconRes)
    }

    private fun applyLanguage() {
        val languageCode = sharedPreferences.getString("languageCode", "fr") ?: "fr"
        setLocale(languageCode)
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(resources.configuration)
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            getString(R.string.lang_french),
            getString(R.string.lang_arabic),
            getString(R.string.lang_english),
            getString(R.string.lang_spanish)
        )
        val languageCodes = arrayOf("fr", "ar", "en", "es")

        val currentLanguage = sharedPreferences.getString("languageCode", "fr") ?: "fr"
        val currentIndex = languageCodes.indexOf(currentLanguage)

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_language))
        builder.setSingleChoiceItems(languages, currentIndex) { dialog, which ->
            val selectedCode = languageCodes[which]

            if (selectedCode != currentLanguage) {
                sharedPreferences.edit().putString("languageCode", selectedCode).apply()

                // Appliquer la langue sans recréer l'activité
                setLocale(selectedCode)

                // Mettre à jour uniquement les textes des tabs
                updateTabTexts()

                dialog.dismiss()

                // Informer l'utilisateur que la langue sera complètement appliquée au prochain démarrage
                Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
            }
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun updateTabTexts() {
        // Mettre à jour les textes des tabs sans recréer l'activité
        for (i in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.let { tab ->
                when (i) {
                    0 -> tab.text = getString(R.string.tab_counter)
                    1 -> tab.text = getString(R.string.tab_map)
                    2 -> tab.text = getString(R.string.tab_profile)
                }
            }
        }
    }

    // Fonction publique pour la déconnexion
    fun performLogout() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.logout))
        builder.setMessage(getString(R.string.logout_message))
        builder.setPositiveButton(getString(R.string.yes)) { _, _ ->
            // Effacer toutes les données utilisateur SAUF les préférences du compteur
            sharedPreferences.edit().apply {
                remove("loggedInUser")
                remove("isLoggedIn")
                // NE PAS effacer les données du compteur
                apply()
            }

            // Rediriger vers l'écran de connexion
            val intent = Intent(this, AuthenticationActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

            Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton(getString(R.string.no)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(true)
        builder.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentTab", viewPager.currentItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val currentTab = savedInstanceState.getInt("currentTab", 0)
        viewPager.currentItem = currentTab
    }
}