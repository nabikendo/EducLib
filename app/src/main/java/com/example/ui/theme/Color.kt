package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * EducLib visual system: an editorial library palette built around warm paper,
 * forest ink and a restrained terracotta accent.
 */
val LibraryForest = Color(0xFF124B3E)
val LibraryForestDeep = Color(0xFF0B3028)
val LibrarySage = Color(0xFFE3EEE8)
val LibrarySageStrong = Color(0xFFD5E7DD)
val LibraryPaper = Color(0xFFF8F6F0)
val LibrarySurface = Color(0xFFFFFFFF)
val LibrarySurfaceAlt = Color(0xFFEEF1EC)
val LibraryInk = Color(0xFF172521)
val LibraryMuted = Color(0xFF40544C)
val LibraryOutline = Color(0xFF7D8B83)
val LibraryControlOutline = Color(0xFF66766E)
val LibraryTerracotta = Color(0xFF91442F)
val LibraryApricot = Color(0xFFF7E5DA)

// Existing screen references are retained as aliases while features migrate to semantic tokens.
val ThemePurplePrimary = LibraryForest
val ThemePurpleLight = LibrarySage
val ThemeOnPrimary = Color.White
val ThemeBackground = LibraryPaper
val ThemeOnBackground = LibraryInk
val ThemeSecondaryText = LibraryMuted
val ThemeBorder = LibraryOutline

// Secondary layout accents, now expressed with the EducLib editorial palette.
val BentoPurpleActive = LibrarySageStrong
val BentoPurpleActiveOn = LibraryForestDeep
val BentoLilacSecondary = LibraryApricot
val BentoLilacSecondaryOn = LibraryInk

// Semantic status highlights; status is also described in text, not only by color.
val StatusAvailableGreenText = Color(0xFF14543D)
val StatusAvailableGreenBg = Color(0xFFDCEEE3)

val StatusBorrowedAmberText = Color(0xFF5A3A00)
val StatusBorrowedAmberBg = Color(0xFFFFF0C2)

val StatusOverdueRedText = Color(0xFF7C2430)
val StatusOverdueRedBg = Color(0xFFF9E2E2)
