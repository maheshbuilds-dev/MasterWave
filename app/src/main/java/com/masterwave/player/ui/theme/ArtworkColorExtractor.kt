package com.masterwave.player.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Complete Dynamic Color Palette extracted from Song Thumbnail:
 * - dominantColor: Prominent base tone of the album cover
 * - vibrantColor: High-energy accent color for interactive controls and highlights
 * - bottomBleedColor: Deep, luxurious tone matched directly to the bottom section of the artwork
 * - ambientGlowColor: Vibrant radiant pool color that creates an atmospheric bloom behind controls
 * - lightVibrantColor: High-key accent for text highlights or scrubber trails
 */
data class ArtworkColorPalette(
    val dominantColor: Color,
    val vibrantColor: Color,
    val bottomBleedColor: Color,
    val ambientGlowColor: Color,
    val lightVibrantColor: Color
)

/**
 * Production Dynamic Artwork Color Extractor for MasterWave Music:
 * - High-performance LRU cache (150 tracks) preventing repeated image decoding.
 * - Extracts bottom slice of artwork to ensure seamless, pixel-perfect downward bleed transitions.
 * - Enforces minimum and maximum luminance/saturation bounds for AMOLED dark readability.
 * - Adapts live to any album cover (local downloaded files or remote streams).
 */
object ArtworkColorExtractor {

    private val colorCache = LruCache<String, Color>(150)
    private val paletteCache = LruCache<String, ArtworkColorPalette>(150)

    suspend fun extractArtworkPalette(context: Context, imageUrl: String?): ArtworkColorPalette? {
        if (imageUrl.isNullOrBlank()) return null

        paletteCache.get(imageUrl)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Software bitmap required for AndroidX Palette & pixel sampling
                    .size(128, 128)
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@withContext null
                    val extracted = extractPaletteFromBitmap(bitmap)
                    paletteCache.put(imageUrl, extracted)
                    colorCache.put(imageUrl, extracted.vibrantColor)
                    extracted
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun extractVibrantColor(context: Context, imageUrl: String?): Color? {
        if (imageUrl.isNullOrBlank()) return null
        colorCache.get(imageUrl)?.let { return it }
        val palette = extractArtworkPalette(context, imageUrl)
        return palette?.vibrantColor
    }

    private fun extractPaletteFromBitmap(bitmap: Bitmap): ArtworkColorPalette {
        val fullPalette = Palette.from(bitmap).generate()

        // 1. Dominant & Vibrant Swatches
        val dominantSwatch = fullPalette.dominantSwatch
        val vibrantSwatch = fullPalette.vibrantSwatch
            ?: fullPalette.darkVibrantSwatch
            ?: fullPalette.lightVibrantSwatch
            ?: dominantSwatch
            ?: fullPalette.mutedSwatch

        // 2. Bottom Slice Color (for Downward Bleed Effect)
        // Crop the bottom 28% of the artwork to sample the exact colors where it bleeds into the canvas
        val sliceStartY = (bitmap.height * 0.72f).toInt().coerceIn(0, bitmap.height - 1)
        val sliceHeight = (bitmap.height - sliceStartY).coerceAtLeast(1)
        val bottomSlice = Bitmap.createBitmap(bitmap, 0, sliceStartY, bitmap.width, sliceHeight)
        val bottomPalette = Palette.from(bottomSlice).generate()

        val bottomSwatch = bottomPalette.dominantSwatch
            ?: bottomPalette.darkVibrantSwatch
            ?: bottomPalette.mutedSwatch
            ?: dominantSwatch
            ?: vibrantSwatch

        // Calculate bottom bleed background color:
        // Rich, atmospheric dark tone that matches the artwork's bottom hue
        val bleedRgb = bottomSwatch?.rgb ?: android.graphics.Color.DKGRAY
        val bleedHsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(bleedRgb),
            android.graphics.Color.green(bleedRgb),
            android.graphics.Color.blue(bleedRgb),
            bleedHsv
        )

        // Keep hue, ensure healthy saturation (at least 0.35 unless pure monochrome),
        // and tune value (brightness) between 0.12 and 0.28 for deep AMOLED depth
        if (bleedHsv[1] > 0.10f && bleedHsv[1] < 0.40f) bleedHsv[1] = 0.45f
        bleedHsv[2] = bleedHsv[2].coerceIn(0.12f, 0.26f)

        val bottomBleedColor = Color(android.graphics.Color.HSVToColor(bleedHsv))

        // 3. Radiant Ambient Glow Color
        val glowRgb = vibrantSwatch?.rgb ?: dominantSwatch?.rgb ?: android.graphics.Color.CYAN
        val glowHsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            android.graphics.Color.red(glowRgb),
            android.graphics.Color.green(glowRgb),
            android.graphics.Color.blue(glowRgb),
            glowHsv
        )
        // Ensure lively saturation and balanced luminescence for the ambient glow pool
        glowHsv[1] = glowHsv[1].coerceIn(0.60f, 0.95f)
        glowHsv[2] = glowHsv[2].coerceIn(0.60f, 0.88f)
        val ambientGlowColor = Color(android.graphics.Color.HSVToColor(glowHsv))

        // 4. Vibrant & Dominant Colors
        val vibrantColor = if (vibrantSwatch != null) {
            Color(vibrantSwatch.rgb)
        } else {
            ambientGlowColor
        }
        val dominantColor = if (dominantSwatch != null) {
            Color(dominantSwatch.rgb)
        } else {
            vibrantColor
        }
        val lightVibrantColor = fullPalette.lightVibrantSwatch?.let { Color(it.rgb) } ?: ambientGlowColor

        return ArtworkColorPalette(
            dominantColor = dominantColor,
            vibrantColor = vibrantColor,
            bottomBleedColor = bottomBleedColor,
            ambientGlowColor = ambientGlowColor,
            lightVibrantColor = lightVibrantColor
        )
    }
}
