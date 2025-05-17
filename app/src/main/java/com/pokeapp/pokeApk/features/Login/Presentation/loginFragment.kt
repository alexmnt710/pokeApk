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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
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

        return view
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
                        Toast.makeText(context, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                        // guardar el usuario que retorne firebase en la base de datos local
                        val user = auth.currentUser
                        val email = user?.email
                        val uid = user?.uid

                        lifecycleScope.launch {
                            val nombreDeUsuario = obtenerNombreDeUsuario()
                            val token = obtenerIdToken()

                            val user = User(
                                uid = uid.toString(),
                                email = email,
                                username = nombreDeUsuario,
                                token = token
                            )
                            val db = AppDatabase.getInstance(requireContext())
                            db.usuarioDao().insertUser(user)
                            Log.d("LoginFragment", "Usuario insertado: $user")

                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                findNavController().navigate(
                                    R.id.action_loginFragment_to_homeFragment,
                                    null,
                                    NavOptions.Builder()
                                        .setPopUpTo(R.id.loginFragment, true)
                                        .build()
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

