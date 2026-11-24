package com.masterwave.player.data.listentogether

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeHelper {
    const val DOMAIN_BASE = "https://masterwave.vercel.app"

    fun buildInviteUrl(roomCode: String): String {
        val clean = roomCode.filter { it.isDigit() }
        return "$DOMAIN_BASE/join?code=$clean"
    }

    fun buildCustomSchemeUrl(roomCode: String): String {
        val clean = roomCode.filter { it.isDigit() }
        return "masterwave://join?code=$clean"
    }

    /**
     * Extracts a 6-digit room code from a URL, deep link, or raw scanned text.
     */
    fun extractRoomCode(rawText: String?): String? {
        if (rawText.isNullOrBlank()) return null
        val trimmed = rawText.trim()

        // 1. Direct 6-digit numeric match
        if (trimmed.length == 6 && trimmed.all { it.isDigit() }) {
            return trimmed
        }

        // 2. Try URI parsing
        try {
            val uri = Uri.parse(trimmed)
            val codeParam = uri.getQueryParameter("code")
            if (!codeParam.isNullOrBlank()) {
                val digits = codeParam.filter { it.isDigit() }
                if (digits.length == 6) return digits
            }
            // Check path segments (e.g. /join/482193)
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                val digits = lastSegment.filter { it.isDigit() }
                if (digits.length == 6) return digits
            }
        } catch (_: Exception) {}

        // 3. Fallback: regex for exactly 6 consecutive digits
        val match = Regex("""\b([0-9]{6})\b""").find(trimmed)
        return match?.groupValues?.get(1)
    }

    /**
     * Generates a high-resolution QR Code Bitmap using ZXing.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.TRANSPARENT
    ): Bitmap {
        val hints = java.util.EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.MARGIN, 1)
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
        }
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) darkColor else lightColor)
            }
        }
        return bitmap
    }
}
