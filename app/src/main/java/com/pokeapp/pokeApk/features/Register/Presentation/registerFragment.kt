package com.pokeapp.pokeApk.features.Register.Presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {
    //Se declaran los elementos de la vista
    private lateinit var inpUser: TextInputLayout
    private lateinit var inpPassword: TextInputLayout
    private lateinit var inpEmail: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var inpConfirmPassword: TextInputLayout
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var redLogin: TextView
    private lateinit var progressBar: ProgressBar


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_register, container, false)
        //Se declaran los elementos de la vista
        inpUser = view.findViewById(R.id.usernameLayout)
        inpPassword = view.findViewById(R.id.passwordLayout)
        inpEmail = view.findViewById(R.id.emailLayout)
        inpConfirmPassword = view.findViewById(R.id.confirmPasswordLayout)
        emailEditText = view.findViewById(R.id.email)
        confirmPasswordEditText = view.findViewById(R.id.confirmPassword)
        usernameEditText = view.findViewById(R.id.username)
        passwordEditText = view.findViewById(R.id.password)
        btnRegister = view.findViewById(R.id.registerButton)
        redLogin = view.findViewById(R.id.tvHaveAccount)
        progressBar = view.findViewById(R.id.progressBar)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var auth = FirebaseAuth.getInstance()

        //Se asigna el evento al boton
        btnRegister.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val email = emailEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            //validar los campos
            if (username.isEmpty()) {
                inpUser.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                inpEmail.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            //validar que el email tenga un formato correcto
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inpEmail.error = "Por favor, ingrese un correo electrónico válido"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                inpPassword.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            //validar que la contraseñas coincidan
            if (password != confirmPassword) {
                inpConfirmPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }
            //iniciar el progreso
            progressBar.visibility = View.VISIBLE
            // Armar el objeto de usuario
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val userMap = hashMapOf(
                            "username" to username,
                            "email" to email,

                        )

                        uid?.let {
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(it)
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                    } else {
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthUserCollisionException -> "El correo electrónico ya está en uso"
                            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil"
                            else -> "Error al registrar: ${task.exception?.localizedMessage}"
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                    progressBar.visibility = View.GONE
                }

        }
        //logica para cuando se presiona el texto de tengo cuenta
        redLogin.setOnClickListener {
            // Navegar a la pantalla de inicio de sesión
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

}

