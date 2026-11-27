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
