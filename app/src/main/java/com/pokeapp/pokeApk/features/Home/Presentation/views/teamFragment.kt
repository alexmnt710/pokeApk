package com.pokeapp.pokeApk.features.Home.Presentation.views

import TeamAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokeapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pokeapp.pokeApk.core.services.NetworkModule
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.pokeapp.pokeApk.data.repository.PokeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class TeamFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TeamAdapter
    private lateinit var txtEmptyTeam: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del fragmento
        return inflater.inflate(R.layout.fragment_team, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnUserMenu = view.findViewById<ImageButton>(R.id.btnUserMenu)
        txtEmptyTeam = view.findViewById(R.id.txtEmptyTeam)


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
                                        AppDatabase.getInstance(requireContext()).usuarioDao()
                                            .eliminarUsuario()
                                    }
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT)
                                        .show()
                                    findNavController().navigate(
                                        R.id.loginFragment, null,
                                        NavOptions.Builder().setPopUpTo(R.id.teamFragment, true)
                                            .build()
                                    )
                                }
                                true
                            }

                            else -> false
                        }
                    }

                    popupMenu.show()
                }
                val btnHome = view?.findViewById<ImageButton>(R.id.btnHome)
                btnHome?.setOnClickListener {
                    findNavController().navigate(R.id.homeFragment)
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
//
//                            else -> false
//                        }
//                    }
//                    popupMenu.show()
                }

            }
        }

        recyclerView = view.findViewById(R.id.rvTeam)
        adapter = TeamAdapter { pokemon ->
            mostrarOffcanvas(pokemon)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

    // Cargar equipo del usuario
        lifecycleScope.launch {
            val pokemones = withContext(Dispatchers.IO) {
                val dao = AppDatabase.getInstance(requireContext()).usuarioDao()
                dao.getUser()?.pokemones ?: emptyList()
            }
            txtEmptyTeam.visibility = if (pokemones.isEmpty()) View.VISIBLE else View.GONE

            val user = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).usuarioDao().getUser()
            }
            Log.d("Usuario", "Pokémon del usuario:")
            user?.pokemones?.forEachIndexed { index, p ->
                Log.d("Usuario", "${index + 1}. ${p.name} (id: ${p.id})")
            }

            Log.d("TeamFragment", "Pokemones: $pokemones")
            adapter.submitList(pokemones)
        }

    }
    private suspend fun recargarEquipoDesdeRoom() {
        val pokemones = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(requireContext()).usuarioDao().getUser()?.pokemones ?: emptyList()
        }
        adapter.submitList(pokemones)
    }
    private fun mostrarOffcanvas(pokemonInicial: PokemonEntity) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.modal_team_pokemon, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.imgAnimatedPokemon)
        val txtNivel = dialogView.findViewById<TextView>(R.id.txtNivel)
        val progress = dialogView.findViewById<ProgressBar>(R.id.progresoNivel)
        val btnAlimentar = dialogView.findViewById<Button>(R.id.btnAlimentar)
        val txtBayasDisponibles = dialogView.findViewById<TextView>(R.id.txtBayasDisponibles)

        val db = AppDatabase.getInstance(requireContext())
        val userDao = db.usuarioDao()
        val firestore = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val idOriginal = pokemonInicial.id

        val pokeRepository = PokeRepository(
            NetworkModule.providePokeApi(NetworkModule.provideRetrofit(NetworkModule.provideOkHttp())),
            db.pokemonDao()
        )

        var pokemonActual = pokemonInicial.copy()
        var nivel = pokemonActual.nivel
        var exp = pokemonActual.exp

        Glide.with(requireContext()).asGif()
            .load("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/${pokemonActual.id}.gif")
            .into(imageView)

        txtNivel.text = "Nivel: $nivel"
        progress.progress = exp

        var bayas = 0

        lifecycleScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId).get().await()
                bayas = (snapshot.get("farm.bayasUsuario") as? Long ?: 0L).toInt()
                txtBayasDisponibles.text = "Bayas disponibles: $bayas"
            } catch (e: Exception) {
                txtBayasDisponibles.text = "Error al cargar bayas"
            }
            val user = withContext(Dispatchers.IO) {
                userDao.getUser()
            }
            val indexEnEquipo = user?.pokemones?.indexOfFirst {
                it.id == pokemonInicial.id && it.nivel == pokemonInicial.nivel && it.exp == pokemonInicial.exp
            } ?: -1

            btnAlimentar.setOnClickListener {
                if (bayas <= 0) {
                    Toast.makeText(requireContext(), "No tienes bayas", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                bayas -= 1
                exp += 20
                txtBayasDisponibles.text = "Bayas disponibles: $bayas"
                progress.progress = exp

                if (exp >= 100) {
                    nivel++
                    exp = 0
                    txtNivel.text = "Nivel: $nivel"
                    progress.progress = 0

                    lifecycleScope.launch {
                        try {
                            val evoId = pokeRepository.puedeEvolucionar(pokemonActual.id)
                            if (evoId != null) {
                                val nuevoPokemon = pokeRepository.getPokemon(evoId)

                                if (nuevoPokemon.id == pokemonActual.id) {
                                    pokemonActual = pokemonActual.copy(nivel = nivel, exp = exp)
                                    Toast.makeText(requireContext(), "¡${pokemonActual.name} ha subido de nivel!", Toast.LENGTH_SHORT).show()
                                } else {
                                    pokemonActual = nuevoPokemon.copy(
                                        nivel = nivel,
                                        exp = exp,
                                        evolucionado = true
                                    )
                                    Glide.with(requireContext()).asGif()
                                        .load("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/showdown/${pokemonActual.id}.gif")
                                        .into(imageView)

                                    Toast.makeText(requireContext(), "¡${pokemonActual.name} ha evolucionado!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                pokemonActual = pokemonActual.copy(nivel = nivel, exp = exp)
                                Toast.makeText(requireContext(), "¡${pokemonActual.name} ha subido de nivel!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: retrofit2.HttpException) {
                            Log.e("Evolucion", "Error HTTP al intentar evolucionar: ${e.code()}")
                            Toast.makeText(requireContext(), "${pokemonActual.name} subio de nivel", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("Evolucion", "Error inesperado: ${e.localizedMessage}")
                            Toast.makeText(requireContext(), "Error inesperado en evolución", Toast.LENGTH_SHORT).show()
                        }

                        guardarEnLocalYFirebase(
                            actualizado = pokemonActual,
                            userDao = userDao,
                            firestore = firestore,
                            userId = userId,
                            index = indexEnEquipo,
                            bayas = bayas
                        )
                    }
                } else {
                    lifecycleScope.launch {
                        pokemonActual = pokemonActual.copy(nivel = nivel, exp = exp)
                        guardarEnLocalYFirebase(
                            actualizado = pokemonActual,
                            userDao = userDao,
                            firestore = firestore,
                            userId = userId,
                            index = indexEnEquipo,
                            bayas = bayas
                        )
                    }
                }
            }
            val modalDialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            modalDialog.show()

            val btnEliminar = dialogView.findViewById<Button>(R.id.btnEliminar)
            btnEliminar.setOnClickListener {
                if (indexEnEquipo != -1) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("¿Eliminar Pokémon?")
                        .setMessage("¿Estás seguro de que quieres eliminar a ${pokemonInicial.name} de tu equipo?")
                        .setPositiveButton("Sí") { _, _ ->
                            lifecycleScope.launch {
                                eliminarPokemonDeEquipo(userDao, firestore, userId, indexEnEquipo, bayas)
                                modalDialog.dismiss()

                                // Verificar si ya no hay pokemones
                                val pokemones = withContext(Dispatchers.IO) {
                                    AppDatabase.getInstance(requireContext()).usuarioDao().getUser()?.pokemones ?: emptyList()
                                }
                                txtEmptyTeam.visibility = if (pokemones.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    Toast.makeText(requireContext(), "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                }
            }

        }


    }
    private suspend fun eliminarPokemonDeEquipo(
        userDao: com.pokeapp.pokeApk.data.localDatabase.dao.userDao,
        firestore: FirebaseFirestore,
        userId: String,
        index: Int,
        bayas: Int
    ) {
        val user = withContext(Dispatchers.IO) {
            userDao.getUser()
        }

        val listaActualizada = user?.pokemones?.toMutableList()
        listaActualizada?.removeAt(index)

        withContext(Dispatchers.IO) {
            userDao.insertUser(user!!.copy(pokemones = listaActualizada ?: listOf()))
        }

        firestore.collection("users").document(userId).update(
            mapOf(
                "farm.bayasUsuario" to bayas,
                "pokemones" to listaActualizada?.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "spriteUrl" to it.spriteUrl,
                        "types" to it.types,
                        "nivel" to it.nivel,
                        "exp" to it.exp,
                        "evolucionado" to it.evolucionado
                    )
                }
            )
        )

        recargarEquipoDesdeRoom()
    }



    private suspend fun guardarEnLocalYFirebase(
        actualizado: PokemonEntity,
        userDao: com.pokeapp.pokeApk.data.localDatabase.dao.userDao,
        firestore: FirebaseFirestore,
        userId: String,
        index: Int,
        bayas: Int
    ) {
        val user = withContext(Dispatchers.IO) {
            userDao.getUser()
        }

        val listaActualizada = user?.pokemones?.toMutableList() ?: mutableListOf()

        if (index in listaActualizada.indices) {
            listaActualizada[index] = actualizado
        }

        withContext(Dispatchers.IO) {
            userDao.insertUser(user!!.copy(pokemones = listaActualizada))
        }

        firestore.collection("users").document(userId).update(
            mapOf(
                "farm.bayasUsuario" to bayas,
                "pokemones" to listaActualizada.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "spriteUrl" to it.spriteUrl,
                        "types" to it.types,
                        "nivel" to it.nivel,
                        "exp" to it.exp,
                        "evolucionado" to it.evolucionado
                    )
                }
            )
        )

        recargarEquipoDesdeRoom()
    }



}
