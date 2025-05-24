package com.pokeapp.pokeApk.util

import com.example.pokeapp.R

object TypeIconMapper {
    private val typeToIconMap = mapOf(
        "fire" to R.drawable.fire,
        "water" to R.drawable.water,
        "grass" to R.drawable.grass,
        "electric" to R.drawable.electric,
        "ice" to R.drawable.ice,
        "fighting" to R.drawable.fighting,
        "poison" to R.drawable.poison,
        "ground" to R.drawable.ground,
        "flying" to R.drawable.flying,
        "psychic" to R.drawable.psychic,
        "bug" to R.drawable.bug,
        "rock" to R.drawable.rock,
        "ghost" to R.drawable.ghost,
        "dragon" to R.drawable.dragon,
        "dark" to R.drawable.dark,
        "steel" to R.drawable.steel,
        "fairy" to R.drawable.fairy,
        "normal" to R.drawable.normal
    )

    fun getIconRes(type: String): Int {
        return typeToIconMap[type.lowercase()] ?: R.drawable.normal
    }
}
