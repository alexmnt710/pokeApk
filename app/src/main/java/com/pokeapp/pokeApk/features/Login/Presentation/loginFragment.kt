package com.pokeapp.pokeApk.features.Login.Presentation

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import com.pokeapp.pokeApk.data.localDatabase.model.PokemonEntity
import com.pokeapp.pokeApk.data.localDatabase.model.User
import com.pokeapp.pokeApk.features.global.obtenerIdToken
import com.pokeapp.pokeApk.features.global.obtenerNombreDeUsuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private lateinit var emailLayout : TextInputLayout
    private lateinit var passwordLayout : TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var btnLogin : Button
    private lateinit var redRegister : TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        auth = FirebaseAuth.getInstance()

        //Se declaran los elementos de la vista
        emailLayout = view.findViewById(R.id.emailLayout)
        passwordLayout = view.findViewById(R.id.passwordLayout)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        btnLogin = view.findViewById(R.id.loginButton)
        redRegister = view.findViewById(R.id.tvNoAccount)
        progressBar = view.findViewById(R.id.progressBar)
        val navHostFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        //logica para cuando el usuario ya esta logueado
        if (auth.currentUser != null) {
            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext()).usuarioDao().getUser()
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
                            AppDatabase.getInstance(requireContext()).usuarioDao().eliminarUsuario()
                        }
                    }
                }
            }
        }

        return view
    }

    suspend fun validarToken(token: String): Boolean {
        val db = FirebaseFirestore.getInstance()
        return try {
            val docRef = db.collection("tokens").document(token)
            val document = docRef.get().await()
            document.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error al validar el token: ${e.message}")
            false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        //Se asigna el evento al boton
        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            //validar los campos
            if (email.isEmpty()) {
                emailLayout.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            //validar el formato del email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Por favor, ingrese un correo electrónico válido"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordLayout.error = "Por favor, complete este campo"
                return@setOnClickListener
            }

            //iniciar el progreso
            progressBar.visibility = View.VISIBLE
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Inicio de sesión exitoso
                        // guardar el usuario que retorne firebase en la base de datos local
                        val user = auth.currentUser
                        val email = user?.email
                        val uid = user?.uid

                        lifecycleScope.launch {
                            val nombreDeUsuario = obtenerNombreDeUsuario()
                            val token = obtenerIdToken()

                            val firestore = FirebaseFirestore.getInstance()
                            val userDoc = firestore.collection("users").document(uid!!)

                            // Obtener o inicializar farm
                            val snapshot = userDoc.get().await()
                            val profileImageUrl = snapshot.getString("profileImage") ?: ""
                            if (!snapshot.contains("farm")) {
                                val farmInit = mapOf(
                                    "bayasUsuario" to 0,
                                    "pasosDesdeUltimaBaya" to 0        // Valor del paso total del sensor en la última generación de bayas

                                )
                                userDoc.set(mapOf("farm" to farmInit), SetOptions.merge()).await()
                                Log.d("Login", "Sistema de farmeo inicializado.")
                            } else {
                                Log.d("Login", "Sistema de farmeo ya existía.")
                            }

                            // Leer campos farm
                            val farm = snapshot.get("farm") as? Map<*, *> ?: emptyMap<String, Any>()

                            // Leer equipo desde Firebase
                            val pokemonesRemotos = snapshot.get("pokemones") as? List<Map<String, Any>> ?: emptyList()

                            val equipo = pokemonesRemotos.map {
                                PokemonEntity(
                                    id = (it["id"] as Long).toInt(),
                                    name = it["name"] as String,
                                    spriteUrl = it["spriteUrl"] as? String,
                                    types = (it["types"] as? List<*>)?.mapNotNull { tipo -> tipo?.toString() } ?: emptyList(),

                                    // Añadir campos nuevos:
                                    nivel = (it["nivel"] as? Long ?: 1L).toInt(),
                                    exp = (it["exp"] as? Long ?: 0L).toInt(),
                                    evolucionado = it["evolucionado"] as? Boolean ?: false

                                )
                            }

                            val user = User(
                                uid = uid.toString(),
                                email = email,
                                username = nombreDeUsuario,
                                token = token,
                                pokemones = equipo,
                                bayasUsuario = (farm["bayasUsuario"] as? Long ?: 0L).toInt(),
                                pasosDesdeUltimaBaya = (farm["pasosDesdeUltimaBaya"] as? Long ?: 0L).toInt(),
                                profileImage = profileImageUrl
                            )

                            val db = AppDatabase.getInstance(requireContext())
                            db.usuarioDao().insertUser(user)

                            Log.d("LoginFragment", "Usuario insertado: $user")
                            Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                navController.navigate(
                                    R.id.homeFragment, null,
                                    NavOptions.Builder().setPopUpTo(R.id.loginFragment, true).build()
                                )
                            }
                        }


                    } else {
                        // Error en el inicio de sesión
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Error al iniciar sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                    //detener el progreso

                }


        }
        //logica para cuando se presiona el texto de no tengo cuenta
        redRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }


        }

    }

