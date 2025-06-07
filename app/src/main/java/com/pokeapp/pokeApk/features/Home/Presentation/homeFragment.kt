package com.pokeapp.pokeApk.features.Home.Presentation

import PokemonAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import androidx.appcompat.widget.SearchView
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
import com.google.firebase.firestore.FirebaseFirestore
import com.pokeapp.pokeApk.core.services.NetworkModule
import com.pokeapp.pokeApk.data.localDatabase.dao.userDao
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.pokeapp.pokeApk.data.localDatabase.model.SimplePokemon
import com.pokeapp.pokeApk.data.repository.PokeRepository
import com.pokeapp.pokeApk.util.TypeIconMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PokemonAdapter
    private lateinit var btnUserMenu: ImageButton
    private lateinit var pgProgrss : ProgressBar
    private lateinit var btnSeeMore : Button
    private var isLoading = false
    private var offset = 0
    private val limit = 20
    private var hasMore = true
    private var dialogDetalle: AlertDialog? = null
    private var currentJob: Job? = null


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
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val btnReset = view.findViewById<ImageButton>(R.id.btnReset)
        val retrofit = NetworkModule.provideRetrofit(NetworkModule.provideOkHttp())
        val api = NetworkModule.providePokeApi(retrofit)
        val db = AppDatabase.getInstance(requireContext())
        val repo = PokeRepository(api, db.pokemonDao())

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchPokemon(it.lowercase()) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false // No buscar en tiempo real
            }
        })

        btnReset.setOnClickListener {
            searchView.setQuery("", false)
            searchView.clearFocus()

            adapter = PokemonAdapter()
            recyclerView.adapter = adapter
            adapter.onSeeMoreClick = { pokemon -> mostrarModalDetalle(pokemon) }

            offset = 0
            hasMore = true
            loadMorePokemons()
        }

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

        //logica de redireccionamiento si no hay usuario
        lifecycleScope.launch {

            val user = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).usuarioDao().getUser()
            }

            if (user == null) {
                findNavController().navigate(R.id.loginFragment)
            }
        }
    }
    private fun searchPokemon(nombre: String) {
        currentJob?.cancel()

        currentJob = lifecycleScope.launch {
            try {
                val repo = PokeRepository(
                    NetworkModule.providePokeApi(NetworkModule.provideRetrofit(NetworkModule.provideOkHttp())),
                    AppDatabase.getInstance(requireContext()).pokemonDao()
                )

                val index = withContext(Dispatchers.IO) {
                    repo.api.getIndex(100_000).results
                }

                val coincidencias = index.filter { it.name.contains(nombre) }

                if (coincidencias.isEmpty()) {
                    Toast.makeText(requireContext(), "Pokémon no encontrado", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (coincidencias.size == 1) {
                    val id = coincidencias.first().url.trimEnd('/').split("/").last().toInt()
                    val pokemon = withContext(Dispatchers.IO) { repo.getPokemon(id) }

                    reiniciarAdaptador()
                    adapter.addPokemon(pokemon)
                    offset = 0
                    hasMore = false
                    return@launch
                }

                // Mostrar diálogo con múltiples opciones
                val nombres = coincidencias.map { it.name.replace("-", " ").replaceFirstChar { it.uppercaseChar() } }.toTypedArray()
                AlertDialog.Builder(requireContext())
                    .setTitle("Elige una variante")
                    .setItems(nombres) { _, which ->
                        val elegido = coincidencias[which]
                        lifecycleScope.launch {
                            val id = elegido.url.trimEnd('/').split("/").last().toInt()
                            val pokemon = withContext(Dispatchers.IO) { repo.getPokemon(id) }

                            reiniciarAdaptador()
                            adapter.addPokemon(pokemon)
                            offset = 0
                            hasMore = false
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            } catch (e: CancellationException) {
                Log.w("Search", "Búsqueda cancelada")
            } catch (e: Exception) {
                Log.e("Search", "Error buscando Pokémon", e)
                Toast.makeText(requireContext(), "Error buscando Pokémon", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun reiniciarAdaptador() {
        adapter = PokemonAdapter()
        recyclerView.adapter = adapter
        adapter.onSeeMoreClick = { pokemon -> mostrarModalDetalle(pokemon) }
    }


    suspend fun agregarPokemonConAlerta(
        context: Context,
        userDao: userDao,
        nuevo: PokemonEntity
    ) {
        val user = withContext(Dispatchers.IO) {
            userDao.getUser()
        }

        if (user == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val listaActual = user.pokemones?.toMutableList() ?: mutableListOf()

        if (listaActual.size < 6) {
            listaActual.add(nuevo)
            withContext(Dispatchers.IO) {
                userDao.insertUser(user.copy(pokemones = listaActual))
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Pokémon agregado", Toast.LENGTH_SHORT).show()
            }
            syncPokemonesConFirebase(user.uid, listaActual)
        } else {
            withContext(Dispatchers.Main) {
                AlertDialog.Builder(context)
                    .setTitle("Límite alcanzado")
                    .setMessage("Ya tienes 6 Pokémon. ¿Deseas reemplazar al primero con este nuevo?")
                    .setPositiveButton("Sí") { _, _ ->
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                listaActual.removeAt(0)
                                listaActual.add(nuevo)
                                userDao.insertUser(user.copy(pokemones = listaActual))

                                val usuario = withContext(Dispatchers.IO) {
                                    userDao.getUser()
                                }

                                Log.d("Usuario", "Pokémon del usuario:")
                                usuario?.pokemones?.forEachIndexed { index, p ->
                                    Log.d("Usuario", "${index + 1}. ${p.name} (id: ${p.id})")
                                }
                                syncPokemonesConFirebase(user.uid, listaActual)

                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Pokémon reemplazado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        }
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

    fun syncPokemonesConFirebase(userId: String, pokemones: List<PokemonEntity>) {
        val firestore = FirebaseFirestore.getInstance()

        val listaFirebase = pokemones.map {
            SimplePokemon(
                id = it.id,
                name = it.name,
                spriteUrl = it.spriteUrl,
                types = it.types
            )
        }

        val updateData = mapOf("pokemones" to listaFirebase)

        firestore.collection("users")
            .document(userId)
            .update(updateData)
            .addOnSuccessListener {
                Log.d("Firebase", "Pokémon del usuario actualizados correctamente.")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error al actualizar pokemones", e)
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
                //imagen de perfil
                Log.d("User", "Profile Image: ${user.profileImage}")


                    user?.let {
                        // Supongamos que user tiene un campo profileImageUrl
                        val profileImageUrl = user.profileImage ?: ""

                        Glide.with(this@HomeFragment)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.user)
                            .error(R.drawable.user)
                            .circleCrop()
                            .into(btnUserMenu)

                        btnUserMenu.setOnClickListener {
                            val popup = PopupMenu(requireContext(), btnUserMenu)
                            popup.menuInflater.inflate(R.menu.user_menu, popup.menu)
                            popup.menu.findItem(R.id.item_username).title = "Usuario: ${user.username}"
                            popup.menu.findItem(R.id.item_email).title = "Correo: ${user.email}"
                            popup.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.item_logout -> {
                                        lifecycleScope.launch {
                                            withContext(Dispatchers.IO) {
                                                AppDatabase.getInstance(requireContext()).usuarioDao().eliminarUsuario()
                                            }
                                            Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                            findNavController().navigate(
                                                R.id.loginFragment,
                                                null,
                                                NavOptions.Builder().setPopUpTo(R.id.farmFragment, true).build()
                                            )
                                        }
                                        true
                                    }
                                    else -> false
                                }
                            }
                            popup.show()
                        }
                    }

                val btnPokemon = view?.findViewById<ImageButton>(R.id.btnPokeball)
                btnPokemon?.setOnClickListener {
                    findNavController().navigate(R.id.teamFragment)
                }
                val btnLevel = view?.findViewById<ImageButton>(R.id.btnLevel)
                btnLevel?.setOnClickListener {
                    findNavController().navigate(R.id.farmFragment)
//                    val popupMenu = PopupMenu(requireContext(), btnLevel)
//                    popupMenu.menuInflater.inflate(R.menu.nav_menu, popupMenu.menu)
//                    popupMenu.setOnMenuItemClickListener { item ->
//                        when (item.itemId) {
//                            R.id.item_farm -> {
//                                findNavController().navigate(R.id.farmFragment)
//                                true
//                            }
//                            R.id.item_movs -> {
//                                findNavController().navigate(R.id.movsFragment)
//                                true
//                            }
//                            else -> false
//                        }
//                    }
//                    popupMenu.show()
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

            val sizeInDp = 60
            val scale = resources.displayMetrics.density
            val sizeInPx = (sizeInDp * scale + 0.5f).toInt()

            val params = LinearLayout.LayoutParams(sizeInPx, sizeInPx)
            params.setMargins(8, 8, 8, 8)
            icon.layoutParams = params

            containerTipos.addView(icon)
        }

        val db = AppDatabase.getInstance(requireContext())
        val userDao = db.usuarioDao()

        val btnAgregar = dialogView.findViewById<Button>(R.id.btnAgregar)
        btnAgregar.setOnClickListener {
            lifecycleScope.launch {
                agregarPokemonConAlerta(requireContext(), userDao, pokemon)
                dialogDetalle?.dismiss()
            }
        }

        dialogDetalle = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogDetalle?.show()
    }


}
