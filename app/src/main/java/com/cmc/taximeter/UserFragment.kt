package com.cmc.taximeter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder

class UserFragment : Fragment() {

    private lateinit var userName: TextView
    private lateinit var userAge: TextView
    private lateinit var userPermis: TextView
    private lateinit var userQRCodeImageView: ImageView
    private lateinit var btnEdit: Button

    private var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Récupérer l'email de l'utilisateur
        userEmail = arguments?.getString("userEmail")

        // Initialiser les vues
        userName = view.findViewById(R.id.userName)
        userAge = view.findViewById(R.id.userAge)
        userPermis = view.findViewById(R.id.userPermis)
        userQRCodeImageView = view.findViewById(R.id.userQRCodeImageView)
        btnEdit = view.findViewById(R.id.btnEdit)

        // Charger les données utilisateur
        loadUserData()

        // Générer le QR Code
        generateQRCode()

        // Configurer le bouton de modification
        btnEdit.setOnClickListener {
            openEditProfile()
        }
    }

    private fun loadUserData() {
        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

        val email = userEmail ?: sharedPreferences.getString("loggedInUser", "") ?: ""
        val name = sharedPreferences.getString("${email}_name", getString(R.string.default_user_name)) ?: getString(R.string.default_user_name)
        val age = sharedPreferences.getString("${email}_age", "24") ?: "24"
        val permis = sharedPreferences.getString("${email}_permis", "Permis B") ?: "Permis B"

        userName.text = name
        userAge.text = "$age ${getString(R.string.years_old)}"
        userPermis.text = permis
    }

    private fun generateQRCode() {
        try {
            val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
            val email = userEmail ?: sharedPreferences.getString("loggedInUser", "") ?: ""
            val name = sharedPreferences.getString("${email}_name", "User") ?: "User"

            // Créer les données du QR Code
            val qrData = """
                Nom: $name
                Email: $email
                Type: Chauffeur de Taxi
                ID: ${email.hashCode()}
            """.trimIndent()

            val multiFormatWriter = MultiFormatWriter()
            val bitMatrix: BitMatrix = multiFormatWriter.encode(
                qrData,
                BarcodeFormat.QR_CODE,
                400,
                400
            )

            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.createBitmap(bitMatrix)

            userQRCodeImageView.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openEditProfile() {
        val intent = Intent(requireContext(), EditProfileActivity::class.java)
        intent.putExtra("userEmail", userEmail)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Recharger les données au retour de l'édition
        loadUserData()
        generateQRCode()
    }
}