package com.pokeapp.pokeApk.data.localDatabase.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pokeapp.pokeApk.data.localDatabase.dao.userDao
import com.pokeapp.pokeApk.data.localDatabase.model.User

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): userDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokeapp_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}