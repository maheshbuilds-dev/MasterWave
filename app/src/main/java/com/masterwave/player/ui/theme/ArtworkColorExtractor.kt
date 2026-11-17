package com.masterwave.player.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production Artwork Color Extractor for MasterWave Music:
 * - Downsamples image to 96x96 for low memory & CPU consumption on Moto G45 5G.
 * - Extracts vibrant, muted and dominant swatches via AndroidX Palette.
 * - Enforces minimum and maximum luminance bounds to preserve contrast & readability.
 */
object ArtworkColorExtractor {

    suspend fun extractVibrantColor(context: Context, imageUrl: String?): Color? {
        if (imageUrl.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Software bitmap required for Palette
                    .size(96, 96)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
                    extractFromBitmap(bitmap)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun extractFromBitmap(bitmap: Bitmap): Color {
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.mutedSwatch
            ?: palette.lightVibrantSwatch

        if (swatch != null) {
            val red = android.graphics.Color.red(swatch.rgb)
            val green = android.graphics.Color.green(swatch.rgb)
            val blue = android.graphics.Color.blue(swatch.rgb)

            // Convert to HSV to ensure color is vibrant enough and not muddy grey or blinding white
            val hsv = FloatArray(3)
            android.graphics.Color.RGBToHSV(red, green, blue, hsv)

            // Clamp saturation between 0.55 and 1.0 for energetic, clear UI accents
            if (hsv[1] < 0.55f) hsv[1] = 0.65f

            // Clamp value (brightness) between 0.45 and 0.88 to maintain contrast on both dark and light
            hsv[2] = hsv[2].coerceIn(0.45f, 0.88f)

            val adjustedRgb = android.graphics.Color.HSVToColor(hsv)
            return Color(adjustedRgb)
        }

        return Color(0xFF165B33) // Fallback to MasterWave primary green
    }
}
