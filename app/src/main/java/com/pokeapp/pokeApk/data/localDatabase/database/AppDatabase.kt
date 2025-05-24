package com.pokeapp.pokeApk.data.localDatabase.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pokeapp.pokeApk.data.localDatabase.dao.PokeDao
import com.pokeapp.pokeApk.data.localDatabase.dao.userDao
import com.pokeapp.pokeApk.data.localDatabase.model.Converters
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntityConverters
import com.pokeapp.pokeApk.data.localDatabase.model.User

@Database(entities = [User::class, PokemonEntity::class], version = 5, exportSchema = false)@TypeConverters(Converters::class, PokemonEntityConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): userDao
    abstract fun pokemonDao(): PokeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokeapp_database"
                )
                .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}