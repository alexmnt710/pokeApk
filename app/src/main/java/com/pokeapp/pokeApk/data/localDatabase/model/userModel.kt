package com.pokeapp.pokeApk.data.localDatabase.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// User data class
@Entity(tableName = "user")
data class User(
    @PrimaryKey val uid: String,
    val email: String?,
    val username: String?,
    val token : String?,
)