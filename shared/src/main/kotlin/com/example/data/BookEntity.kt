package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String,
    val isbn: String = "",
    val genre: String,
    val year: Int,
    val location: String,
    val photoPath: String?, // Local file path of the saved photo
    val totalQuantity: Int,
    val availableQuantity: Int,
    
    // Novas colunas integradas para JTA e ROOF
    val lastLoanDate: Long? = null,
    val totalLoans: Int = 0,
    val useFrequency: Int = 0,
    val category: String = "",
    val tipoObra: String = "Literário", // Didático, Literário, Referência, Técnico
    val publicoAlvo: String = "Geral", // Infantil, Infanto-Juvenil, Jovem Adulto, Adulto
    val classificacaoJTA: String? = null, // Baixa circulação estrutural, Baixa circulação situacional, Baixa circulação crítica, Normal
    val textoJustificativa: String? = null,
    val posicaoEstanteId: Int? = null
)
