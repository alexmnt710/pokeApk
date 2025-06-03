package com.pokeapp.pokeApk.features.Home.Presentation.views

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FarmFragment : Fragment(), SensorEventListener {

    private lateinit var imgArbol: ImageView
    private lateinit var txtBayas: TextView
    private lateinit var txtBayasTotales: TextView
    private lateinit var txtPasos: TextView
    private lateinit var txtPasosRestantes: TextView
    private lateinit var progressBar: View
    private lateinit var prefs: SharedPreferences

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var baseStepCount: Int = -1
    private lateinit var uid: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_farm, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar componentes de vista
        imgArbol = view.findViewById(R.id.imgArbol)
        txtBayas = view.findViewById(R.id.txtBayas)
        txtBayasTotales = view.findViewById(R.id.txtBayasTotales)
        txtPasos = view.findViewById(R.id.txtPasos)
        progressBar = view.findViewById(R.id.progressFarm)
        txtPasosRestantes = view.findViewById(R.id.txtPasosRestantes)

        prefs = requireContext().getSharedPreferences("pokePrefs", android.content.Context.MODE_PRIVATE)
        baseStepCount = prefs.getInt("baseStepCount", -1)

        // Inicializar sensor
        sensorManager = requireContext().getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        initStepSensor()

        // UID Firebase actual
        uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        setupButtons(view)
        actualizarEstado()

        imgArbol.setOnClickListener {
            recolectarBayas(uid)
        }
    }

    private fun initStepSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 1000)
                return
            }
        }
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    private fun setupButtons(view: View) {
        val btnUserMenu = view.findViewById<ImageButton>(R.id.btnUserMenu)
        val btnHome = view.findViewById<ImageButton>(R.id.btnHome)
        val btnTeam = view.findViewById<ImageButton>(R.id.btnPokeball)

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext()).usuarioDao().getUser()
            }

            user?.let {
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
        }

        btnHome.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        btnTeam.setOnClickListener {
            findNavController().navigate(R.id.teamFragment)
        }
    }

    private fun obtenerImagenArbolPorBayas(bayasEnArbol: Int): Int {
        return when (bayasEnArbol) {
            in 0..2 -> R.drawable.arbol_1
            in 3..5 -> R.drawable.arbol_2
            in 6..9 -> R.drawable.arbol_3
            else -> R.drawable.arbol_4
        }
    }


    private fun actualizarEstado() {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("users").document(uid).get().await()

                val bayasTotales = (snapshot.get("farm.bayasUsuario") as? Long ?: 0L).toInt()
                val pasosDesdeUltima = (snapshot.get("farm.pasosDesdeUltimaBaya") as? Long ?: 0L).toInt()
                val bayasEnArbol = pasosDesdeUltima / 5
                val pasosRestantes = pasosDesdeUltima % 5

                txtBayasTotales.text = "x$bayasTotales"
                txtBayas.text = "Bayas en el árbol: $bayasEnArbol"
                txtPasosRestantes.text = "Pasos para la próxima baya: ${5 - pasosRestantes}"

                imgArbol.setImageResource(obtenerImagenArbolPorBayas(bayasEnArbol))
            } catch (e: Exception) {
                Log.e("Farm", "Error actualizando estado", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }


    private fun recolectarBayas(userId: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val ref = FirebaseFirestore.getInstance().collection("users").document(userId)
                val snapshot = ref.get().await()
                val farm = snapshot.get("farm") as? Map<*, *> ?: return@launch

                val pasosActuales = prefs.getInt("pasosDesdeUltimaBaya", 0)
                val bayasGeneradas = pasosActuales / 5
                val pasosRestantes = pasosActuales % 5

                txtPasosRestantes.text = "Pasos para la próxima baya: ${5 - pasosRestantes}"
                txtBayas.text = "Bayas en el árbol: $bayasGeneradas"
                imgArbol.setImageResource(obtenerImagenArbolPorBayas(bayasGeneradas))

                if (bayasGeneradas > 0) {
                    val nuevosDatos = mapOf(
                        "farm.bayasUsuario" to (farm["bayasUsuario"] as? Long ?: 0L).toInt() + bayasGeneradas,
                        "farm.pasosDesdeUltimaBaya" to 0
                    )
                    ref.update(nuevosDatos).await()
                    prefs.edit().putInt("pasosDesdeUltimaBaya", 0).apply()
                    pasosDesdeUltimaRecoleccion = 0

                    Toast.makeText(requireContext(), "Recolectaste $bayasGeneradas bayas", Toast.LENGTH_SHORT).show()
                    actualizarEstado()
                } else {
                    Toast.makeText(requireContext(), "Aún no hay suficientes pasos", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("Farm", "Error recolectando bayas", e)
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }



    private var pasosDesdeUltimaRecoleccion = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (!::uid.isInitialized || event == null) return


        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                pasosDesdeUltimaRecoleccion += 1
                val pasosParaSiguiente = 5 - (pasosDesdeUltimaRecoleccion % 5)
                val bayasDisponibles = pasosDesdeUltimaRecoleccion / 5

                prefs.edit().putInt("pasosDesdeUltimaBaya", pasosDesdeUltimaRecoleccion).apply()

                txtPasos.text = "Pasos Totales: $pasosDesdeUltimaRecoleccion"
                txtBayas.text = "Bayas en el árbol: $bayasDisponibles"
                txtPasosRestantes.text = "Pasos para la próxima baya: $pasosParaSiguiente"
                imgArbol.setImageResource(obtenerImagenArbolPorBayas(bayasDisponibles))
            }


            Sensor.TYPE_STEP_COUNTER -> {
                val pasosTotales = event.values[0].toInt()
                if (baseStepCount == -1 || pasosTotales < baseStepCount) {
                    sincronizarBaseStepCount(pasosTotales)
                }
                val pasosReales = (pasosTotales - baseStepCount).coerceAtLeast(0)
                prefs.edit().putInt("pasosReales", pasosReales).apply()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No requerido para TYPE_STEP_COUNTER
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), SensorManager.SENSOR_DELAY_NORMAL)

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        } ?: run {
            Toast.makeText(requireContext(), "Sensor de pasos no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permiso concedido", Toast.LENGTH_SHORT).show()
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            stepSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            Toast.makeText(requireContext(), "Permiso denegado. Habilítalo en Configuración.", Toast.LENGTH_LONG).show()
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }
    }
    private fun sincronizarBaseStepCount(pasosTotales: Int) {
        val guardadoLocal = prefs.getInt("baseStepCount", -1)
        if (guardadoLocal == -1) {
            lifecycleScope.launch {
                try {
                    val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
                    val snapshot = ref.get().await()
                    val firestoreBase = (snapshot.get("farm.baseStepCount") as? Long)?.toInt() ?: -1

                    val base = if (firestoreBase != -1) firestoreBase else pasosTotales

                    // Guardar baseStepCount en ambos lugares
                    prefs.edit().putInt("baseStepCount", base).apply()
                    ref.update("farm.baseStepCount", base).await()

                    baseStepCount = base
                    Log.d("Farm", "baseStepCount sincronizado: $base")
                } catch (e: Exception) {
                    Log.e("Farm", "Error sincronizando baseStepCount", e)
                    // Como fallback guarda el actual
                    prefs.edit().putInt("baseStepCount", pasosTotales).apply()
                    baseStepCount = pasosTotales
                }
            }
        } else {
            baseStepCount = guardadoLocal
        }
    }

}

