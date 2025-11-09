package com.cmc.taximeter

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var editName: TextInputEditText
    private lateinit var editAge: TextInputEditText
    private lateinit var spinnerPermis: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var userEmail: String? = null
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème et la langue AVANT super.onCreate()
        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        applyDarkMode()
        applyLanguage()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Récupérer l'email
        userEmail = intent.getStringExtra("userEmail")

        // Initialiser les vues
        editName = findViewById(R.id.editName)
        editAge = findViewById(R.id.editAge)
        spinnerPermis = findViewById(R.id.spinnerPermis)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // Setup Spinner
        setupSpinner()

        // Charger les données actuelles
        loadCurrentData()

        // Configurer les boutons
        btnSave.setOnClickListener {
            saveProfile()
        }

        btnCancel.setOnClickListener {
            finish()
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

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPermis.adapter = adapter
    }

    private fun loadCurrentData() {
        val email = userEmail ?: sharedPreferences.getString("loggedInUser", "") ?: ""

        val name = sharedPreferences.getString("${email}_name", "")
        val age = sharedPreferences.getString("${email}_age", "")
        val permis = sharedPreferences.getString("${email}_permis", "")

        editName.setText(name)
        editAge.setText(age)

        // Sélectionner le permis dans le spinner
        val licenseTypes = resources.getStringArray(R.array.array  )
        val position = licenseTypes.indexOf(permis)
        if (position >= 0) {
            spinnerPermis.setSelection(position)
        }
    }

    private fun saveProfile() {
        val name = editName.text.toString().trim()
        val age = editAge.text.toString().trim()
        val permis = spinnerPermis.selectedItem.toString()

        if (name.isEmpty() || age.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }

        val email = userEmail ?: sharedPreferences.getString("loggedInUser", "") ?: ""

        sharedPreferences.edit().apply {
            putString("${email}_name", name)
            putString("${email}_age", age)
            putString("${email}_permis", permis)
            apply()
        }

        Toast.makeText(this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show()
        finish()
    }
}