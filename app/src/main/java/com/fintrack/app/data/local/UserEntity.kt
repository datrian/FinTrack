package com.fintrack.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Fila de la tabla "users": un usuario registrado en la base de datos local.
// "passwordHash" nunca guarda la contraseña en texto plano (ver hashPassword).
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String
)
