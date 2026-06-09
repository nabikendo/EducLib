package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// High Contrast Color Palette (Ultra-Legibility Under Fluorescent Lights)
val ThemePurplePrimary = Color(0xFF1A0066) // Deep crisp royal indigo for ultimate button and accent contrast (15:1 ratio)
val ThemePurpleLight = Color(0xFFEADBFF)   // Saturated background accent container (very legible)
val ThemeOnPrimary = Color(0xFFFFFFFF)       // Icons and text on primary buttons
val ThemeBackground = Color(0xFFFFFFFF)    // Absolute crisp pure white backdrop for maximal reflection contrast
val ThemeOnBackground = Color(0xFF0D0D0D)  // Near-pure solid rich dark black for bold headings and body (21:1 contrast)
val ThemeSecondaryText = Color(0xFF2B2B33) // Deep charcoal gray for secondary descriptors (retains high legibility)
val ThemeBorder = Color(0xFF71707E)        // Stronger, darker outline border to prevent clipping under bright light washes

// Secondary layout accent elements (Bento style)
val BentoPurpleActive = Color(0xFFD3BFFF)  // Light purple active container accent
val BentoPurpleActiveOn = Color(0xFF11003D) // Dark violet text for active status
val BentoLilacSecondary = Color(0xFFDDD2F0) // Lilac secondary container accent
val BentoLilacSecondaryOn = Color(0xFF150F21) // Slate purple text for status

// Semantic status highlights
val StatusAvailableGreenText = Color(0xFF0A3622) // Saturated deep green
val StatusAvailableGreenBg = Color(0xFFD1E7DD)

val StatusBorrowedAmberText = Color(0xFF4D3A02) // Saturated deep amber
val StatusBorrowedAmberBg = Color(0xFFFFF3CD)

val StatusOverdueRedText = Color(0xFF842029) // Strong dark crimson
val StatusOverdueRedBg = Color(0xFFF8D7DA)
