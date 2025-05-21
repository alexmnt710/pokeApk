package com.pokeapp.pokeApk.data.localDatabase.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.Json

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

data class SpeciesDto(
    @Json(name = "flavor_text_entries")
    val flavorTextEntries: List<FlavorTextDto>
)
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
    val description: String? = null
)

data class PokemonListResponse(
    val count: Int,
    val results: List<PokemonListItem>
)

data class PokemonListItem(
    val name: String,
    val url: String
)

class Converters {

    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toList(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(",")
}

