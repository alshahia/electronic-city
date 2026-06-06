package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontEntry
import com.example.R

/**
 * D3 — Google Fonts with layered fallback.
 *
 * Layered fallback chain:
 *   1. Google Fonts (network) — IBM Plex Sans Arabic (display) + IBM Plex Sans (body / Latin)
 *   2. Bundled .ttf in res/font/ (offline — same font family, no network needed)
 *   3. System font (last resort)
 *
 * Compose picks the first Font in the family that has the requested glyph, so
 * mixing Arabic + Latin fonts in a single FontFamily gives bilingual rendering
 * for free. The bundled fonts ensure the app looks identical offline vs online.
 *
 * Total bundled font size: ~2.1 MB (4 weights × 2 families).
 */
private val gmsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val plexArabic = GoogleFont("IBM Plex Sans Arabic")
private val plexLatin = GoogleFont("IBM Plex Sans")

/** Display — used for headlines, titles, large numeric/Arabic text. */
val DisplayFont: FontFamily = FontFamily(
    // Tier 1: Google Fonts (network)
    GoogleFontEntry(googleFont = plexArabic, fontProvider = gmsProvider, weight = FontWeight.Bold, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexArabic, fontProvider = gmsProvider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexLatin, fontProvider = gmsProvider, weight = FontWeight.Bold, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexLatin, fontProvider = gmsProvider, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexArabic, fontProvider = gmsProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    // Tier 2: Bundled .ttf (offline — same font family)
    Font(R.font.ibm_plex_sans_arabic_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_arabic_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_arabic_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    // Tier 3: System font (implicit fallback — always present)
)

/** Body — used for paragraphs, buttons, labels. */
val BodyFont: FontFamily = FontFamily(
    // Tier 1: Google Fonts (network)
    GoogleFontEntry(googleFont = plexArabic, fontProvider = gmsProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexLatin, fontProvider = gmsProvider, weight = FontWeight.Normal, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexArabic, fontProvider = gmsProvider, weight = FontWeight.Medium, style = FontStyle.Normal),
    GoogleFontEntry(googleFont = plexLatin, fontProvider = gmsProvider, weight = FontWeight.Medium, style = FontStyle.Normal),
    // Tier 2: Bundled .ttf (offline — same font family)
    Font(R.font.ibm_plex_sans_arabic_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(R.font.ibm_plex_sans_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    // Tier 3: System font (implicit fallback — always present)
)
