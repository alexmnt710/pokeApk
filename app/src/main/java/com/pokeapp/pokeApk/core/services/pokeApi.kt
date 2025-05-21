package com.pokeapp.pokeApk.core.services

import com.pokeapp.pokeApk.data.localDatabase.model.IndexResponse
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonDetailDto
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonListResponse
import com.pokeapp.pokeApk.data.localDatabase.model.SpeciesDto
import okhttp3.OkHttpClient

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApiService {
    @GET("pokemon")
    suspend fun getIndex(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0
    ): IndexResponse

    @GET("pokemon/{id}")
    suspend fun getPokemon(
        @Path("id") id: Int
    ): PokemonDetailDto

    @GET("pokemon-species/{id}")
    suspend fun getSpecies(
        @Path("id") id: Int
    ): SpeciesDto

    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int = 0
    ): PokemonListResponse
}
