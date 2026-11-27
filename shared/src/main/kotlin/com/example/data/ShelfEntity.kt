package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Estante física.
 *
 * Os campos de layout descrevem a estante na planta da biblioteca (metros), usados pelo mapa 3D.
 * `mapX`/`mapZ` nulos significam que a estante ainda não foi posicionada: o mapa a coloca numa
 * posição estimada. O referencial da planta é definido pelos dois pontos de referência marcados
 * na varredura AR (origem no primeiro ponto, eixo X apontando para o segundo).
 */
@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    /** Centro da base da estante na planta, em metros. */
    val mapX: Double? = null,
    val mapZ: Double? = null,
    /** Direção da frente da estante na planta, em graus (0 = frente voltada para +Z). */
    val rotationDeg: Double = 0.0,
    val widthM: Double = DEFAULT_WIDTH_M,
    val depthM: Double = DEFAULT_DEPTH_M,
    val heightM: Double = DEFAULT_HEIGHT_M,
    /** Quantidade de prateleiras (níveis verticais); a prateleira 1 é a de baixo. */
    val levelCount: Int = DEFAULT_LEVEL_COUNT,
    /** Quantidade de colunas/módulos lado a lado; a coluna 1 é a da esquerda olhando de frente. */
    val columnCount: Int = 1,
    /** Origem das medidas: "" (padrão), "manual" ou "ar". */
    val layoutSource: String = ""
) {
    /**
     * Garante que a grade da estante comporte a célula informada. Sem isso, "Coluna 2" numa estante de
     * 1 coluna era descartada pelo mapa e o destaque cobria a prateleira inteira.
     */
    fun expandedToFit(level: Int, column: Int?): ShelfEntity {
        val levels = maxOf(levelCount, level).coerceAtMost(MAX_LEVELS)
        val columns = maxOf(columnCount, column ?: 1).coerceAtMost(MAX_COLUMNS)
        return if (levels == levelCount && columns == columnCount) this else copy(levelCount = levels, columnCount = columns)
    }

    /** Medidas que o mapa consegue desenhar e o backup JSON consegue gravar (sem NaN/infinito). */
    fun hasValidLayout(): Boolean {
        fun Double.inRange(min: Double, max: Double) = isFinite() && this > min && this <= max
        val coordsOk = listOfNotNull(mapX, mapZ).all { it.isFinite() && kotlin.math.abs(it) <= 1_000.0 }
        return coordsOk && rotationDeg.isFinite() &&
            widthM.inRange(0.0, MAX_DIMENSION_M) &&
            depthM.inRange(0.0, MAX_DIMENSION_M) &&
            heightM.inRange(0.0, MAX_DIMENSION_M) &&
            levelCount in 1..MAX_LEVELS &&
            columnCount in 1..MAX_COLUMNS
    }

    companion object {
        const val DEFAULT_WIDTH_M = 1.0
        const val DEFAULT_DEPTH_M = 0.35
        const val DEFAULT_HEIGHT_M = 1.8
        const val DEFAULT_LEVEL_COUNT = 5
        const val MAX_LEVELS = 30
        const val MAX_COLUMNS = 50
        const val MAX_DIMENSION_M = 50.0
    }
}
