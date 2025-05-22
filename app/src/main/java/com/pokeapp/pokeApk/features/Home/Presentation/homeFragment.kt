package com.pokeapp.pokeApk.features.Home.Presentation

import PokemonAdapter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
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
import com.pokeapp.pokeApk.data.repository.PokeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PokemonAdapter
    private lateinit var btnUserMenu: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.rvPokemons)
        btnUserMenu = view.findViewById(R.id.btnUserMenu)
        adapter = PokemonAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).usuarioDao().getUser()
            }

            if (user != null) {
                Log.d("User", "Username: ${user.username}")
                Log.d("User", "Email: ${user.email}")

                btnUserMenu.setOnClickListener {
                    val popupMenu = PopupMenu(requireContext(), btnUserMenu)
                    popupMenu.menuInflater.inflate(R.menu.user_menu, popupMenu.menu)

                    // Personalizar con datos del usuario
                    popupMenu.menu.findItem(R.id.item_username).title = "Usuario: ${user.username}"
                    popupMenu.menu.findItem(R.id.item_email).title = "Correo: ${user.email}"

                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.item_logout -> {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        AppDatabase.getInstance(requireContext()).usuarioDao().eliminarUsuario()
                                    }
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(
                                        R.id.action_homeFragment_to_loginFragment, null,
                                        NavOptions.Builder().setPopUpTo(R.id.homeFragment, true).build()
                                    )
                                }
                                true
                            }
                            else -> false
                        }
                    }

                    popupMenu.show()
                }

                val retrofit = NetworkModule.provideRetrofit(NetworkModule.provideOkHttp())
                val api = NetworkModule.providePokeApi(retrofit)
                val db = AppDatabase.getInstance(requireContext())
                val repo = PokeRepository(api, db.pokemonDao())

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

                            adapter.addPokemon(pokemon)
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
        }
    }
}
