package com.masterwave.player.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masterwave.player.ui.theme.DisplayFontFamily
import com.masterwave.player.ui.theme.rememberAppThemeColors
import com.masterwave.player.ui.theme.WebAccentEmerald
import com.masterwave.player.ui.theme.WebAccentGreen
import com.masterwave.player.ui.theme.WebAccentOchre
import com.masterwave.player.ui.theme.WebAccentPeach
import com.masterwave.player.ui.theme.WebAccentPink
import com.masterwave.player.ui.theme.WebAccentRose
import com.masterwave.player.ui.theme.WebAccentSky
import com.masterwave.player.ui.theme.WebAccentViolet

/**
 * Supercool Support MasterWave Screen:
 * Styled with MasterWave Website Warm Light Aesthetic (#fffaf0 canvas, tactile bento cards,
 * bold ink outlines, drop-shadows & pastel badges).
 * Leverages deep psychological reciprocity, transparent open-source principles,
 * and direct one-tap UPI contribution integration.
 */
@Composable
fun SupportCommunityScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = rememberAppThemeColors()
    val primaryUpiId = "masterwave.app@axl"

    var selectedTierAmount by remember { mutableStateOf("150") }

    fun copyUpi() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MasterWave UPI ID", primaryUpiId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "UPI ID copied: $primaryUpiId", Toast.LENGTH_SHORT).show()
    }

    fun openUpiIntent(amount: String) {
        val upiUri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", primaryUpiId)
            .appendQueryParameter("pn", "MasterWave Music")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", "Support MasterWave Indie Music")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, upiUri)
        val chooser = Intent.createChooser(intent, "Pay ₹$amount using:")
        try {
            context.startActivity(chooser)
        } catch (_: Exception) {
            copyUpi()
            Toast.makeText(context, "No UPI app found. Copied UPI ID: $primaryUpiId", Toast.LENGTH_LONG).show()
        }
    }

    fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Top Bar ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, colors.border, CircleShape)
                        .background(colors.surfaceSoft, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Support MasterWave",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colors.ink
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentPink)
                                .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "100% INDIE",
                                fontFamily = DisplayFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                    }
                    Text(
                        text = "Fuel independent, ad-free studio music forever",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        color = colors.muted
                    )
                }
            }
        }

        // --- Hero Psychological Creed Card ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentEmerald)
                                .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ZERO ADS • ZERO TRACKERS",
                                fontFamily = DisplayFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = WebAccentPink, modifier = Modifier.size(24.dp))
                    }

                    Text(
                        text = "Sound that stays pure.\nPowered by listeners like you.",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        color = colors.ink
                    )

                    Text(
                        text = "Corporate streaming giants lock high-fidelity audio behind expensive monthly subscriptions, disrupt your listening with loud ads, and sell your private habits to marketing algorithms.\n\nMasterWave was built on a different principle: pure studio sound should be free, private, and accessible to everyone on earth.",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = colors.body
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.canvas,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = colors.ink, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Zero corporate investors. Zero venture capital. 100% funded by passionate listeners who believe in open art.",
                                fontFamily = DisplayFontFamily,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink
                            )
                        }
                    }
                }
            }
        }

        // --- Reciprocity Call to Action & Contribution Tiers ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Choose Your Contribution",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.ink
                    )
                    Text(
                        text = "If MasterWave has given you peace during long commutes or focus during deep study, consider giving back the cost of a single cup of chai. Every rupee directly covers high-speed audio bandwidth.",
                        fontFamily = DisplayFontFamily,
                        fontSize = 13.sp,
                        color = colors.body,
                        lineHeight = 18.sp
                    )

                    // Bento Grid of Contribution Options
                    val tiers = listOf(
                        Triple("₹50", "Chai Tip • Fuel an hour of code", WebAccentPeach),
                        Triple("₹150", "Audiophile Patron • Covers 10k lyrics", WebAccentViolet),
                        Triple("₹500", "Bandwidth Hero • High-speed servers", WebAccentSky),
                        Triple("₹1000", "Core Sponsor • Open-source guardian", WebAccentOchre)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tiers.forEach { (amount, label, accent) ->
                            val isSelected = selectedTierAmount == amount.removePrefix("₹")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) colors.surfaceSoft else colors.canvas)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.5.dp,
                                        color = if (isSelected) colors.border else colors.hairline,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedTierAmount = amount.removePrefix("₹") }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                            .border(1.5.dp, colors.border, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "₹", fontFamily = DisplayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0A0A0A))
                                    }
                                    Column {
                                        Text(
                                            text = amount,
                                            fontFamily = DisplayFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = colors.ink
                                        )
                                        Text(
                                            text = label,
                                            fontFamily = DisplayFontFamily,
                                            fontSize = 12.sp,
                                            color = colors.muted
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(2.dp, colors.border, CircleShape)
                                        .background(if (isSelected) colors.border else Color.Transparent, CircleShape)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    // Direct One-Tap UPI Action Button
                    Button(
                        onClick = { openUpiIntent(selectedTierAmount) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF165B33),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Contribute ₹$selectedTierAmount via UPI",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // --- Instant UPI Details Card ---
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(2.dp, colors.border),
                shadowElevation = if (colors.isDark) 0.dp else 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Instant India UPI Payment",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = colors.ink
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(WebAccentGreen)
                                .border(1.5.dp, colors.border, RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VERIFIED",
                                fontFamily = DisplayFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0A0A0A)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.canvas,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = primaryUpiId,
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.ink
                                )
                                Text(
                                    text = "Any UPI App: GPay • PhonePe • Paytm • CRED",
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.muted
                                )
                            }
                            Button(
                                onClick = { copyUpi() },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.surfaceSoft,
                                    contentColor = colors.ink
                                ),
                                modifier = Modifier.border(1.5.dp, colors.border, RoundedCornerShape(999.dp)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", fontFamily = DisplayFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Compatible with Google Pay, PhonePe, Paytm, BHIM, Cred, and Amazon Pay.",
                        fontFamily = DisplayFontFamily,
                        fontSize = 12.sp,
                        color = colors.muted
                    )
                }
            }
        }

        // --- Official Website & Community Links ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceSoft,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Official MasterWave Links",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.ink
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openBrowser("https://masterwave.vercel.app") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "https://masterwave.vercel.app",
                            fontFamily = DisplayFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openBrowser("https://masterwave-music.vercel.app") },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "https://masterwave-music.vercel.app",
                            fontFamily = DisplayFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = colors.ink, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
