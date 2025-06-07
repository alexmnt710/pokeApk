package com.pokeapp.pokeApk.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.pokeapp.pokeApk.core.services.PokeApiService
import com.pokeapp.pokeApk.data.localDatabase.dao.PokeDao
import com.pokeapp.pokeApk.data.localDatabase.model.EvolutionChainDto
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import kotlinx.coroutines.flow.Flow

class PokeRepository(
    internal val api: PokeApiService,
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

        val detail = api.getPokemon(id)

        val species = try {
            api.getSpecies(id)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) {
                null // Forma alterna, no tiene especie propia
            } else {
                throw e
            }
        }

        val spanish = species?.flavorTextEntries
            ?.firstOrNull { it.language.name == "es" }
            ?.text
            ?.replace("\n", " ")
            ?.replace("\u000c", " ")

        val entity = PokemonEntity(
            id = detail.id,
            name = detail.name,
            spriteUrl = detail.sprites.other.officialArtwork.url,
            types = detail.types.sortedBy { it.slot }.map { it.type.name },
            description = spanish ?: "Descripción no disponible para esta forma."
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

    suspend fun puedeEvolucionar(pokemonId: Int): Int? {
        val species = api.getSpecies(pokemonId)
        val evolutionChainUrl = species.evolutionChain.url
        val evolutionId = evolutionChainUrl.trimEnd('/').split("/").last().toInt()
        val chain = api.getEvolutionChain(evolutionId)

        fun encontrarEvolucion(cadena: EvolutionChainDto.Chain, objetivo: String): EvolutionChainDto.Chain? {
            if (cadena.species.name == objetivo) return cadena
            for (rama in cadena.evolvesTo) {
                val encontrada = encontrarEvolucion(rama, objetivo)
                if (encontrada != null) return encontrada
            }
            return null
        }

        val speciesDetail = api.getPokemon(pokemonId)
        val nodo = encontrarEvolucion(chain.chain, speciesDetail.name)
        val siguiente = nodo?.evolvesTo?.firstOrNull()?.species?.url ?: return null

        return siguiente.trimEnd('/').split("/").last().toInt()
    }
    suspend fun getIdByName(name: String): Int? {
        val response = api.getIndex(100_000)
        return response.results.firstOrNull { it.name == name.lowercase() }
            ?.url?.trimEnd('/')?.split("/")?.last()?.toInt()
    }


}


