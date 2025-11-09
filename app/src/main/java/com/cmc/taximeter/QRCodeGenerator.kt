package com.cmc.taximeter

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QRCodeGenerator {

    /**
     * Génère un QR code à partir d'un texte/données
     * @param content Le contenu à encoder dans le QR code
     * @param width Largeur du QR code en pixels
     * @param height Hauteur du QR code en pixels
     * @param foregroundColor Couleur du QR code (par défaut noir)
     * @param backgroundColor Couleur de fond (par défaut blanc)
     * @return Bitmap du QR code généré
     */
    fun generateQRCode(
        content: String,
        width: Int = 512,
        height: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            // Configuration des hints pour le QR code
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // Haute correction d'erreurs
                put(EncodeHintType.MARGIN, 1) // Marge autour du QR code
            }

            // Création du QR code
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            // Conversion en Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) foregroundColor else backgroundColor
                    )
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Génère un QR code pour un utilisateur avec format JSON
     * @param userId ID de l'utilisateur
     * @param name Nom de l'utilisateur
     * @param email Email de l'utilisateur
     * @param permis Type de permis
     * @return Bitmap du QR code
     */
    fun generateUserQRCode(
        userId: String,
        name: String,
        email: String,
        permis: String
    ): Bitmap? {
        // Création d'un JSON avec les informations de l'utilisateur
        val userInfo = """
            {
                "userId": "$userId",
                "name": "$name",
                "email": "$email",
                "permis": "$permis",
                "timestamp": ${System.currentTimeMillis()}
            }
        """.trimIndent()

        return generateQRCode(userInfo, 512, 512)
    }

    /**
     * Génère un QR code simple avec l'ID de l'utilisateur
     * @param userId ID de l'utilisateur
     * @return Bitmap du QR code
     */
    fun generateSimpleUserQRCode(userId: String): Bitmap? {
        // Format simple : TAXIMETER-USERID-TIMESTAMP
        val content = "TAXIMETER-$userId-${System.currentTimeMillis()}"
        return generateQRCode(content, 512, 512)
    }
}