// MainActivity.kt
package com.example.pokeapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Obtener el NavController desde el NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Verificar el token y navegar al fragmento correspondiente
        verificarTokenYRedirigir()
    }

    private fun verificarTokenYRedirigir() {
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).usuarioDao().getUser()
            }

            if (user != null && !user.token.isNullOrEmpty()) {
                val tokenValido = validarToken(user.token)
                if (tokenValido) {
                    // Navegar al HomeFragment
                    navController.navigate(R.id.homeFragment, null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                    )
                } else {
                    // Token inválido, eliminar usuario y navegar al LoginFragment
                    withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(applicationContext).usuarioDao().eliminarUsuario()
                    }
                    Toast.makeText(
                        this@MainActivity,
                        "Sesión expirada. Por favor, inicia sesión nuevamente.",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate(R.id.loginFragment,null,
                        navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build()
                    )
                }
            } else {
                // No hay usuario, navegar al LoginFragment
                navController.navigate(R.id.loginFragment,null,
                    navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .build()
                    )
            }
        }
    }

    private suspend fun validarToken(token: String): Boolean {
        return try {
            val currentUser = auth.currentUser
            val idTokenResult = currentUser?.getIdToken(false)?.await()
            idTokenResult?.token == token
        } catch (e: Exception) {
            false
        }
    }
}
