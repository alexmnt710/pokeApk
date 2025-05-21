package com.pokeapp.pokeApk.data.localDatabase.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity

@Dao
interface PokeDao {

    @Query("SELECT * FROM pokemon_index ORDER BY id ASC")
    fun pagingSource(): PagingSource<Int, PokemonEntity>

    @Query("SELECT * FROM pokemon_index WHERE id = :id LIMIT 1")
    suspend fun find(id: Int): PokemonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PokemonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PokemonEntity)


}
