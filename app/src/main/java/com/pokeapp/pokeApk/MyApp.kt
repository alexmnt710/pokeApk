package com.example.pokeapp

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = HashMap<String, String>().apply {
            put("cloud_name", "ddu7bdpgg")
            put("api_key", "879438942877221")
            put("api_secret", "ywdjtNVeufrBu0oTh53kLUTa0C8")
        }

        try {
            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // Ya estaba inicializado, ignoramos
        }
    }
}
