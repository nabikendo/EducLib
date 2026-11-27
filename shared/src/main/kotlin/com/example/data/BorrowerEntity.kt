package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrowers")
data class BorrowerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "Aluno" or "Professor"
    val identifier: String, // Turma or Matrícula/Matéria depending on type
    val phone: String = "",
    val classRoom: String = "", // Representing "Turma" (Class/Grade level)
    val blockedUntil: Long? = null, // New field for block logic
    val photoUri: String? = null
)
