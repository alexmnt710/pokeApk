package com.pokeapp.pokeApk.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pokeapp.pokeApk.core.services.PokeApiService
import com.pokeapp.pokeApk.data.localDatabase.dao.PokeDao
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import kotlinx.coroutines.flow.Flow

class PokeRepository(
    private val api: PokeApiService,
    private val dao: PokeDao
) {

    /**
     * Descarga todos los nombres una sola vez
     */
    suspend fun syncIndex() {
        val response = api.getIndex(limit = 100_000)
        val entities = response.results.map { dto ->
            val id = dto.url.trimEnd('/').split("/").last().toInt()
            PokemonEntity(id = id, name = dto.name, spriteUrl = null)
        }
        dao.insertAll(entities)
    }

    /**
     * Obtiene un Pokémon con su detalle (lo cachea)
     */
    suspend fun getPokemon(id: Int): PokemonEntity {
        dao.find(id)?.let { if (it.spriteUrl != null) return it }

        val detail  = api.getPokemon(id)
        val species = api.getSpecies(id)

        val spanish = species.flavorTextEntries
            .firstOrNull { it.language.name == "es" }?.text
            ?.replace("\n", " ")?.replace("\u000c", " ")

        val entity = PokemonEntity(
            id   = detail.id,
            name = detail.name,
            spriteUrl = detail.sprites.other.officialArtwork.url,
            types = detail.types.sortedBy { it.slot }.map { it.type.name },
            description = spanish
        )
        dao.insert(entity)
        return entity
    }

    fun getAllLocal(): Flow<PagingData<PokemonEntity>> {
        return Pager(
            config = PagingConfig(pageSize = 20),
            pagingSourceFactory = { dao.pagingSource() }
        ).flow
    }

}


