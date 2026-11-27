package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Minimal EducLib palette: soft sage surfaces, warm paper backgrounds and
 * dark, readable ink for comfortable everyday use.
 */
val LibraryForest = Color(0xFF5C7D70)
val LibraryForestDeep = Color(0xFF354F44)
val LibrarySage = Color(0xFFE7EFE9)
val LibrarySageStrong = Color(0xFFD7E5DC)
val LibraryPaper = Color(0xFFF7F7F3)
val LibrarySurface = Color(0xFFFFFFFF)
val LibrarySurfaceAlt = Color(0xFFEDF2EF)
val LibraryInk = Color(0xFF273630)
val LibraryMuted = Color(0xFF50635A)
val LibraryOutline = Color(0xFF7D8B83)
val LibraryControlOutline = Color(0xFF66766E)
val LibraryTerracotta = Color(0xFF866758)
val LibraryApricot = Color(0xFFF3EAE4)

// Existing screen references are retained as aliases while features migrate to semantic tokens.
val ThemePurplePrimary = LibraryForest
val ThemePurpleLight = LibrarySage
val ThemeOnPrimary = Color.White
val ThemeBackground = LibraryPaper
val ThemeOnBackground = LibraryInk
val ThemeSecondaryText = LibraryMuted
val ThemeBorder = LibraryOutline

// Secondary layout accents, expressed with the softened EducLib palette.
val BentoPurpleActive = LibrarySageStrong
val BentoPurpleActiveOn = LibraryForestDeep
val BentoLilacSecondary = LibraryApricot
val BentoLilacSecondaryOn = LibraryInk

// Status is always conveyed with text as well as color.
val StatusAvailableGreenText = Color(0xFF486E59)
val StatusAvailableGreenBg = Color(0xFFE2EEE5)

val StatusBorrowedAmberText = Color(0xFF765C2F)
val StatusBorrowedAmberBg = Color(0xFFF5ECD7)

val StatusOverdueRedText = Color(0xFF7C5057)
val StatusOverdueRedBg = Color(0xFFF3E4E6)
