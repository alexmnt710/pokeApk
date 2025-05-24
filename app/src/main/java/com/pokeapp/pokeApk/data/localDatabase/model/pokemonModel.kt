package com.pokeapp.pokeApk.data.localDatabase.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class IndexResponse(
    val results: List<IndexItemDto>
)
data class IndexItemDto(
    val name: String,
    val url: String
)


data class PokemonDetailDto(
    val id: Int,
    val name: String,
    val sprites: SpritesDto,
    val types: List<TypeSlotDto>
)
data class SpritesDto(
    val other: OtherSpriteDto
)
data class OtherSpriteDto(
    @Json(name = "official-artwork")
    val officialArtwork: OfficialSpriteDto
)
data class OfficialSpriteDto(
    @Json(name = "front_default")
    val url: String?
)
data class TypeSlotDto(
    val slot: Int,
    val type: NamedApiResource
)
data class NamedApiResource(val name: String, val url: String)


data class FlavorTextDto(
    @Json(name = "flavor_text") val text: String,
    val language: NamedApiResource
)


@Entity(tableName = "pokemon_index")
data class PokemonEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val spriteUrl: String?, 
    val types: List<String> = emptyList(),
    val description: String? = null,
    val nivel: Int = 1,
    val exp: Int = 0,
    val evolucionado: Boolean = false
)

data class PokemonListResponse(
    val count: Int,
    val results: List<PokemonListItem>
)

data class PokemonListItem(
    val name: String,
    val url: String
)

data class SimplePokemon(
    val id: Int,
    val name: String,
    val spriteUrl: String? = null,
    val types: List<String> = emptyList()
)
data class EstadoArbol(
    val bayasActuales: Int,
    val fase: Int,
    val faltanMilis: Long
)
data class EvolutionChainDto(
    val id: Int,
    val chain: Chain
) {
    data class Chain(
        val species: NamedApiResource,
        @Json(name = "evolves_to") val evolvesTo: List<Chain>
    )
}
data class SpeciesDto(
    @Json(name = "flavor_text_entries")
    val flavorTextEntries: List<FlavorTextDto>,

    @Json(name = "evolution_chain")
    val evolutionChain: EvolutionChainUrlDto
)

data class EvolutionChainUrlDto(
    val url: String
)

class Converters {

    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(",")
}

class PokemonEntityConverters {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val type = Types.newParameterizedType(List::class.java, PokemonEntity::class.java)
    private val adapter = moshi.adapter<List<PokemonEntity>>(type)

    @TypeConverter
    fun fromPokemonList(pokemones: List<PokemonEntity>?): String {
        return adapter.toJson(pokemones ?: emptyList())
    }

    @TypeConverter
    fun toPokemonList(json: String): List<PokemonEntity> {
        return adapter.fromJson(json) ?: emptyList()
    }
}
