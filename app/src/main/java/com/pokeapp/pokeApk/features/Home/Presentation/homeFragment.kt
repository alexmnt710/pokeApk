package com.pokeapp.pokeApk.features.Home.Presentation

import PokemonAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.pokeapp.R
import com.pokeapp.pokeApk.core.services.NetworkModule
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.pokeapp.pokeApk.data.repository.PokeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)

    }
    lateinit var btnLogout : Button
    lateinit var tvUser: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PokemonAdapter
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        btnLogout = view.findViewById(R.id.btnLogout)

    }

    override fun onStart() {
        super.onStart()

        // Inicializar la vista
        tvUser = requireView().findViewById(R.id.tvUserInfo)
        recyclerView = requireView().findViewById(R.id.rvPokemons)
        adapter = PokemonAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        // Acceder a la base de datos en un hilo de fondo
        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).usuarioDao().getUser()
            }

            // Verificar si se obtuvo un usuario
            if (user != null) {
                Log.d("User", "Username: ${user.username}")
                Log.d("User", "Email: ${user.email}")
                Log.d("User", "Token: ${user.token}")
                Log.d("User", "UID: ${user.uid}")
                // Mostrar la información del usuario en la vista
                val tvUserContent = """
                    Nombre de usuario: ${user.username}
                    Correo electrónico: ${user.email}
                    UID: ${user.uid}
                """.trimIndent()
                tvUser.text = tvUserContent
                val retrofit = NetworkModule.provideRetrofit(NetworkModule.provideOkHttp())
                val api = NetworkModule.providePokeApi(retrofit)
                val db = AppDatabase.getInstance(requireContext())
                val repo       = PokeRepository(api, db.pokemonDao())

                try {
                    withContext(Dispatchers.IO) {
                        repo.syncIndex()
                    }

                    val pokemonList = withContext(Dispatchers.IO) {
                        api.getPokemonList(limit = 20).results
                    }

                    for (item in pokemonList) {
                        try {
                            val id = item.url.trimEnd('/').split("/").last().toInt()
                            val pokemon = withContext(Dispatchers.IO) {
                                repo.getPokemon(id)
                            }

                            adapter.addPokemon(pokemon) // <- Aquí se añade visualmente
                        } catch (e: Exception) {
                            Log.e("Pokemon", "Error con ${item.name}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Pokemon", "Error al obtener datos del Pokémon", e)
                }
            } else {
                Log.d("User", "No se encontró ningún usuario en la base de datos local.")
            }

            btnLogout.setOnClickListener {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        AppDatabase.getInstance(requireContext()).usuarioDao().eliminarUsuario()
                    }
                    // Aquí puedes agregar la lógica para redirigir al usuario a la pantalla de inicio de sesión
                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_homeFragment_to_loginFragment,null,
                        navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.homeFragment, true)
                            .build()
                    )
                }

            }



        }
    }
}
