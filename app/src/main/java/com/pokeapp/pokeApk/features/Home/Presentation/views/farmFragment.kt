package com.pokeapp.pokeApk.features.Home.Presentation.views

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.pokeapp.pokeApk.data.localDatabase.model.EstadoArbol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FarmFragment : Fragment() {
    private lateinit var imgArbol: ImageView
    private lateinit var txtBayas: TextView
    private lateinit var txtContador: TextView
    private var handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private lateinit var uid: String
    private lateinit var txtBayasTotales: TextView
    private lateinit var progressBar: View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del fragmento
        return inflater.inflate(R.layout.fragment_farm, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val btnUserMenu = view.findViewById<ImageButton>(R.id.btnUserMenu)
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
                                        NavOptions.Builder().setPopUpTo(R.id.farmFragment, true)
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
                val btnTeam = view?.findViewById<ImageButton>(R.id.btnPokeball)
                btnTeam?.setOnClickListener {
                    findNavController().navigate(R.id.teamFragment)
                }
                val btnLevel = view?.findViewById<ImageButton>(R.id.btnLevel)
                btnLevel?.setOnClickListener {
                    val popupMenu = PopupMenu(requireContext(), btnLevel)
                    popupMenu.menuInflater.inflate(R.menu.nav_menu, popupMenu.menu)
                    popupMenu.setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.item_movs -> {
                                findNavController().navigate(R.id.movsFragment)
                                true
                            }

                            else -> false
                        }
                    }
                    popupMenu.show()
                }

            }
        }
        imgArbol = view.findViewById(R.id.imgArbol)
        txtBayas = view.findViewById(R.id.txtBayas)
        txtContador = view.findViewById(R.id.txtContador)
        txtBayasTotales = view.findViewById(R.id.txtBayasTotales)
        progressBar = view.findViewById(R.id.progressFarm)


        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        actualizarEstado()
        imgArbol.setOnClickListener {
            recolectarBayas(uid)
        }
    }

    suspend fun obtenerEstadoDelArbol(uid: String): EstadoArbol? {
        val firestore = FirebaseFirestore.getInstance()
        val userDoc = firestore.collection("users").document(uid)

        return try {
            val snapshot = userDoc.get().await()
            val farm = snapshot.get("farm") as? Map<*, *> ?: return null

            val lastHarvestedAt = farm["lastHarvestedAt"] as? Long ?: return null
            val intervaloGeneracion = (farm["intervaloGeneracion"] as? Long) ?: 30000L
            val maxBayas = (farm["maxBayas"] as? Long)?.toInt() ?: 15

            val tiempoActual = System.currentTimeMillis()
            val tiempoPasado = tiempoActual - lastHarvestedAt

            val bayasGeneradas = (tiempoPasado / intervaloGeneracion).toInt()
            val bayasActuales = minOf(bayasGeneradas, maxBayas)

            val faltanMilis = intervaloGeneracion - (tiempoPasado % intervaloGeneracion)

            val fase = when {
                bayasActuales == 0 -> 1
                bayasActuales in 1..4 -> 1
                bayasActuales in 5..9 -> 2
                else -> 3
            }

            EstadoArbol(
                bayasActuales = bayasActuales,
                fase = fase,
                faltanMilis = faltanMilis
            )
        } catch (e: Exception) {
            Log.e("Farm", "Error obteniendo estado del árbol", e)
            null
        }
    }

    fun obtenerImagenArbolPorBayas(bayas: Int): Int {
        return when (bayas) {
            0 -> R.drawable.arbol_1  // 1.png
            in 1..4 -> R.drawable.arbol_2 // 2.png
            in 5..9 -> R.drawable.arbol_3 // 3.png
            else -> R.drawable.arbol_4 // 4.png
        }
    }
    private fun actualizarEstado() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val estado = obtenerEstadoDelArbol(uid) ?: return@launch
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("users").document(uid).get().await()
            val bayasTotales = (snapshot.get("farm.bayasUsuario") as? Long ?: 0L).toInt()
            txtBayasTotales.text = "x$bayasTotales"

            // Imagen
            imgArbol.setImageResource(obtenerImagenArbolPorBayas(estado.bayasActuales))

            // Texto
            txtBayas.text = "Bayas en el árbol: ${estado.bayasActuales}"

            // Contador
            runnable?.let { handler.removeCallbacks(it) }
            runnable = object : Runnable {
                var tiempoRestante = estado.faltanMilis
                override fun run() {
                    txtContador.text = "Siguiente baya en: ${tiempoRestante / 1000}s"
                    tiempoRestante -= 1000
                    if (tiempoRestante > 0) {
                        handler.postDelayed(this, 1000)
                    } else {
                        actualizarEstado() // Recargar automáticamente
                    }
                }
            }
            handler.post(runnable!!)
            progressBar.visibility = View.GONE

        }
    }

    private fun recolectarBayas(userId: String) {
        progressBar.visibility = View.VISIBLE
        val firestore = FirebaseFirestore.getInstance()
        val userDoc = firestore.collection("users").document(userId)

        lifecycleScope.launch {
            try {
                // Obtener documento actual
                val snapshot = userDoc.get().await()
                val farm = snapshot.get("farm") as? Map<*, *> ?: return@launch

                val lastHarvestedAt = farm["lastHarvestedAt"] as? Long ?: return@launch
                val intervalo = (farm["intervaloGeneracion"] as? Long) ?: 30000L
                val maxBayas = (farm["maxBayas"] as? Long)?.toInt() ?: 15
                val bayasUsuario = (farm["bayasUsuario"] as? Long ?: 0L).toInt()

                val tiempoActual = System.currentTimeMillis()
                val tiempoPasado = tiempoActual - lastHarvestedAt
                val bayasGeneradas = (tiempoPasado / intervalo).toInt()
                val bayasAReclamar = minOf(bayasGeneradas, maxBayas)

                if (bayasAReclamar > 0) {
                    val nuevasBayas = bayasUsuario + bayasAReclamar
                    val updates = mapOf(
                        "farm.bayasUsuario" to nuevasBayas,
                        "farm.lastHarvestedAt" to tiempoActual
                    )
                    userDoc.update(updates).await()

                    // Leer actualización
                    txtBayasTotales.text = "x$nuevasBayas"

                    Toast.makeText(requireContext(), "Recolectaste $bayasAReclamar bayas", Toast.LENGTH_SHORT).show()
                    actualizarEstado()
                } else {
                    Toast.makeText(requireContext(), "No hay bayas para recolectar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Farm", "Error al recolectar bayas: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }


}