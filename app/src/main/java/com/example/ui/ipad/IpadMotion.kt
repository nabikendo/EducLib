package com.example.ui.ipad

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** Shared animation tokens for tablet-first editorial UI. */
object IpadMotion {
    const val PressMillis = 110
    const val SelectionMillis = 220
    const val PanelMillis = 320

    fun <T> panel(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    fun <T> selection(): FiniteAnimationSpec<T> = tween(SelectionMillis)
}
