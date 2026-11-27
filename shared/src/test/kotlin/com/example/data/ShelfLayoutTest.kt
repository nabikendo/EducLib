package com.example.data

import com.example.map.CellCoordinate
import com.example.map.layoutShelves
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfLayoutTest {
    @Test
    fun structuredPositionGrowsTheGridSoTheMapHighlightsTheRightCell() {
        val shelf = ShelfEntity(id = 1, name = "Nova", mapX = 0.0, mapZ = 0.0) // 5 prateleiras, 1 coluna
        val grown = shelf.expandedToFit(level = 7, column = 2)
        assertEquals(7, grown.levelCount)
        assertEquals(2, grown.columnCount)
        assertSame(shelf, shelf.expandedToFit(level = 3, column = 1))
        assertEquals(ShelfEntity.MAX_LEVELS, shelf.expandedToFit(level = 99, column = null).levelCount)

        // Com a grade ampliada, a célula (prateleira 7, coluna 2) ocupa só a metade direita do topo.
        val quad = layoutShelves(listOf(grown)).single().cellQuad(CellCoordinate(level = 7, column = 2))
        assertEquals(0.0, quad.minOf { it.x }, 1e-9)
        assertEquals(grown.heightM, quad.maxOf { it.y }, 1e-9)
    }

    @Test
    fun nonFiniteMeasurementsAreInvalidAndRejectedByBackupValidation() {
        val ok = ShelfEntity(id = 1, name = "A")
        assertTrue(ok.hasValidLayout())
        listOf(
            ok.copy(rotationDeg = Double.POSITIVE_INFINITY),
            ok.copy(rotationDeg = Double.NaN),
            ok.copy(widthM = Double.NaN),
            ok.copy(heightM = Double.POSITIVE_INFINITY),
            ok.copy(mapX = Double.NEGATIVE_INFINITY, mapZ = 0.0)
        ).forEach { assertFalse(it.toString(), it.hasValidLayout()) }

        val payload = BackupPayload(
            formatVersion = BackupFormat.CURRENT_FORMAT_VERSION, books = emptyList(), loans = emptyList(), borrowers = emptyList(),
            reservations = emptyList(), shelves = listOf(ok.copy(rotationDeg = Double.POSITIVE_INFINITY)), shelfPositions = emptyList(), weeklySuggestions = emptyList()
        )
        assertNotNull(BackupFormat.validate(payload))
    }
}
