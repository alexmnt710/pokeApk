package com.pokeapp.pokeApk.features.Home.Presentation

import PokemonAdapter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokeapp.R
import com.pokeapp.pokeApk.core.services.NetworkModule
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.pokeapp.pokeApk.data.repository.PokeRepository
import com.pokeapp.pokeApk.util.TypeIconMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PokemonAdapter
    private lateinit var btnUserMenu: ImageButton
    private lateinit var pgProgrss : ProgressBar
    private lateinit var btnSeeMore : Button
    private var selectedPokemon: PokemonEntity? = null
    private var isLoading = false
    private var offset = 0
    private val limit = 20
    private var hasMore = true
    private var dialogDetalle: AlertDialog? = null


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
        adapter.onSeeMoreClick = { pokemon ->
            mostrarModalDetalle(pokemon)
        }

        btnSeeMore = view.findViewById(R.id.btnSeeMore)
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && hasMore && lastVisibleItem >= totalItemCount - 5) {
                    loadMorePokemons()
                }
            }
        })
        pgProgrss = view.findViewById(R.id.progress)
    }

    private fun loadMorePokemons() {
        isLoading = true
        lifecycleScope.launch {
            try {
                val retrofit = NetworkModule.provideRetrofit(NetworkModule.provideOkHttp())
                val api = NetworkModule.providePokeApi(retrofit)
                val db = AppDatabase.getInstance(requireContext())
                val repo = PokeRepository(api, db.pokemonDao())

                val results = withContext(Dispatchers.IO) {
                    api.getPokemonList(limit = limit, offset = offset).results
                }

                if (results.isEmpty()) {
                    hasMore = false
                    return@launch
                }

                for (item in results) {
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

                offset += limit
            } catch (e: Exception) {
                Log.e("Pagination", "Error cargando más Pokémons", e)
            } finally {
                isLoading = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        pgProgrss.visibility = View.VISIBLE

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
                            pgProgrss.visibility = View.GONE

                            adapter.addPokemon(pokemon)
                        } catch (e: Exception) {
                            Log.e("Pokemon", "Error con ${item.name}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Pokemon", "Error al obtener datos del Pokémon", e)
                } finally {
                }


            } else {
                Log.d("User", "No se encontró ningún usuario en la base de datos local.")
            }
        }
    }
    private fun mostrarModalDetalle(pokemon: PokemonEntity) {
        // Cerrar diálogo anterior si está visible
        dialogDetalle?.dismiss()

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.modal_poke, null)

        val imageView = dialogView.findViewById<ImageView>(R.id.imgPokemon)
        val nombre = dialogView.findViewById<TextView>(R.id.txtNombre)
        val descripcion = dialogView.findViewById<TextView>(R.id.txtDescripcion)
        val containerTipos = dialogView.findViewById<LinearLayout>(R.id.containerTipos)
        val btnCerrar = dialogView.findViewById<ImageButton>(R.id.btnCerrar)
        btnCerrar.setOnClickListener {
            dialogDetalle?.dismiss()
        }


        Glide.with(requireContext())
            .load(pokemon.spriteUrl)
            .into(imageView)

        nombre.text = pokemon.name.replaceFirstChar { it.uppercaseChar() }
        descripcion.text = pokemon.description ?: "Sin descripción disponible."

        containerTipos.removeAllViews()
        pokemon.types?.forEach { tipo ->
            val iconRes = TypeIconMapper.getIconRes(tipo)
            val icon = ImageView(requireContext())
            icon.setImageResource(iconRes)

            val sizeInDp = 60 // ajusta a un tamaño más pequeño
            val scale = resources.displayMetrics.density
            val sizeInPx = (sizeInDp * scale + 0.5f).toInt()

            val params = LinearLayout.LayoutParams(sizeInPx, sizeInPx)
            params.setMargins(8, 8, 8, 8)
            icon.layoutParams = params

            containerTipos.addView(icon)
        }

        // Crear y mostrar el diálogo
        dialogDetalle = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogDetalle?.show()
    }


}
