package com.cmc.taximeter

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.*

class AuthenticationActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: android.content.SharedPreferences

    // Views
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPassword: TextInputEditText
    private lateinit var edtName: TextInputEditText
    private lateinit var edtAge: TextInputEditText
    private lateinit var layoutName: TextInputLayout
    private lateinit var layoutAge: TextInputLayout
    private lateinit var spinnerLicenseType: Spinner
    private lateinit var btnSignIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var txtSignUp: TextView
    private lateinit var txtSignIn: TextView

    // Menu buttons
    private lateinit var btnDarkMode: MaterialButton
    private lateinit var btnLanguage: MaterialButton

    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème et la langue AVANT super.onCreate()
        sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        applyDarkMode()
        applyLanguage()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)

        // Vérifier si l'utilisateur est déjà connecté
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            navigateToMainActivity()
            return
        }

        initializeViews()
        setupMenuButtons()
        setupLicenseSpinner()
        setupClickListeners()
    }

    private fun initializeViews() {
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtName = findViewById(R.id.edtName)
        edtAge = findViewById(R.id.edtAge)
        layoutName = findViewById(R.id.layoutName)
        layoutAge = findViewById(R.id.layoutAge)
        spinnerLicenseType = findViewById(R.id.spinnerLicenseType)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignUp = findViewById(R.id.btnSignUp)
        txtSignUp = findViewById(R.id.txtSignUp)
        txtSignIn = findViewById(R.id.txtSignIn)

        btnDarkMode = findViewById(R.id.btnDarkMode)
        btnLanguage = findViewById(R.id.btnLanguage)
    }

    private fun setupMenuButtons() {
        // Dark Mode
        updateDarkModeIcon()
        btnDarkMode.setOnClickListener {
            toggleDarkMode()
        }

        // Language
        btnLanguage.setOnClickListener {
            showLanguageDialog()
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

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_language))
        builder.setSingleChoiceItems(languages, currentIndex) { dialog, which ->
            val selectedCode = languageCodes[which]

            if (selectedCode != currentLanguage) {
                sharedPreferences.edit().putString("languageCode", selectedCode).apply()
                setLocale(selectedCode)
                recreate() // Recréer l'activité pour appliquer la langue
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun setupLicenseSpinner() {
        val licenseTypes = resources.getStringArray(R.array.license_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, licenseTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLicenseType.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSignIn.setOnClickListener {
            if (isSignUpMode) {
                Toast.makeText(this, "Veuillez d'abord passer en mode connexion", Toast.LENGTH_SHORT).show()
            } else {
                signIn()
            }
        }

        btnSignUp.setOnClickListener {
            if (!isSignUpMode) {
                Toast.makeText(this, "Veuillez d'abord passer en mode inscription", Toast.LENGTH_SHORT).show()
            } else {
                signUp()
            }
        }

        txtSignUp.setOnClickListener {
            toggleAuthMode()
        }

        txtSignIn.setOnClickListener {
            toggleAuthMode()
        }
    }

    private fun toggleAuthMode() {
        isSignUpMode = !isSignUpMode

        if (isSignUpMode) {
            // Mode Inscription
            layoutName.visibility = View.VISIBLE
            layoutAge.visibility = View.VISIBLE
            spinnerLicenseType.visibility = View.VISIBLE
            btnSignIn.visibility = View.GONE
            btnSignUp.visibility = View.VISIBLE
            txtSignUp.visibility = View.GONE
            txtSignIn.visibility = View.VISIBLE
        } else {
            // Mode Connexion
            layoutName.visibility = View.GONE
            layoutAge.visibility = View.GONE
            spinnerLicenseType.visibility = View.GONE
            btnSignIn.visibility = View.VISIBLE
            btnSignUp.visibility = View.GONE
            txtSignUp.visibility = View.VISIBLE
            txtSignIn.visibility = View.GONE
        }
    }

    private fun signIn() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        // Vérifier si l'utilisateur existe
        val savedPassword = sharedPreferences.getString("${email}_password", null)

        if (savedPassword == null) {
            Toast.makeText(this, "Cet utilisateur n'existe pas", Toast.LENGTH_SHORT).show()
            return
        }

        if (savedPassword != password) {
            Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
            return
        }

        // Connexion réussie
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("loggedInUser", email)
            apply()
        }
        Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()
        navigateToMainActivity()
    }

    private fun signUp() {
        val email = edtEmail.text.toString().trim()
        val password = edtPassword.text.toString().trim()
        val name = edtName.text.toString().trim()
        val ageStr = edtAge.text.toString().trim()
        val licenseType = spinnerLicenseType.selectedItem.toString()

        if (email.isEmpty() || password.isEmpty() || name.isEmpty() || ageStr.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }

        val age = ageStr.toIntOrNull()
        if (age == null || age < 18) {
            Toast.makeText(this, "Âge invalide (minimum 18 ans)", Toast.LENGTH_SHORT).show()
            return
        }

        // Vérifier si l'utilisateur existe déjà
        val existingPassword = sharedPreferences.getString("${email}_password", null)
        if (existingPassword != null) {
            Toast.makeText(this, "Cet email est déjà utilisé", Toast.LENGTH_SHORT).show()
            return
        }

        // Sauvegarder les données de l'utilisateur
        sharedPreferences.edit().apply {
            putString("${email}_password", password)
            putString("${email}_name", name)
            putString("${email}_age", age.toString())
            putString("${email}_permis", licenseType)
            putBoolean("isLoggedIn", true)
            putString("loggedInUser", email)
            apply()
        }

        Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show()
        navigateToMainActivity()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}