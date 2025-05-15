package com.pokeapp.pokeApk.data.localDatabase.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pokeapp.pokeApk.data.localDatabase.model.User

@Dao
interface userDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)
    @Query("SELECT * FROM user LIMIT 1")
    fun getUser(): User?
    //eliminar usuario
    @Query ("DELETE FROM user")
    suspend fun eliminarUsuario()
}