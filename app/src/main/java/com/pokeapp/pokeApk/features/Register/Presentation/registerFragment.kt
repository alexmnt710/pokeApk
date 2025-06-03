package com.pokeapp.pokeApk.features.Register.Presentation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File


class RegisterFragment : Fragment() {

    private lateinit var inpUser: TextInputLayout
    private lateinit var inpPassword: TextInputLayout
    private lateinit var inpEmail: TextInputLayout
    private lateinit var inpConfirmPassword: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var btnRegister: Button
    private lateinit var redLogin: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var profileImage: CircleImageView

    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var imageUri: Uri? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingAction: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

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
        profileImage = view.findViewById(R.id.profileImage)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = FirebaseAuth.getInstance()

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[android.Manifest.permission.CAMERA] ?: true
            val storageGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                ?: permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: true

            if (cameraGranted && storageGranted) {
                when (pendingAction) {
                    "camera" -> launchCamera()
                    "gallery" -> launchGallery()
                }
            } else {
                Toast.makeText(requireContext(), "Permisos denegados", Toast.LENGTH_SHORT).show()
            }
        }
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let { uri ->
                    profileImage.setImageURI(uri)

                    val file = File(requireContext().cacheDir, "profile_picture.jpg")
                    Log.d("RegisterFragment", "¿Se tomó la foto?: $success")
                    Log.d("RegisterFragment", "Archivo existe: ${file.exists()}")
                    Log.d("RegisterFragment", "Tamaño del archivo: ${file.length()} bytes")
                }
            } else {
                Log.e("RegisterFragment", "No se tomó la foto o fue cancelada")
            }
        }


        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                profileImage.setImageURI(it)
                imageUri = it
            }
        }

        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        btnRegister.setOnClickListener {
            inpUser.error = null
            inpEmail.error = null
            inpPassword.error = null
            inpConfirmPassword.error = null

            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            val email = emailEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (username.isEmpty()) {
                inpUser.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                inpEmail.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                inpEmail.error = "Por favor, ingrese un correo electrónico válido"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                inpPassword.error = "Por favor, complete este campo"
                return@setOnClickListener
            }
            if (password != confirmPassword) {
                inpConfirmPassword.error = "Las contraseñas no coinciden"
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE


            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Ya hay sesión, puedes subir imagen y luego guardar datos
                            uploadProfileImage(imageUri, user.uid, onSuccess = { url ->
                                val userMap = mapOf(
                                    "username" to username,
                                    "email" to email,
                                    "profileImage" to url
                                )
                                saveUserToFirestore(user.uid, userMap as HashMap<String, String>)
                            }, onError = {
                                // Manejo de error en subida de imagen
                            })
                        } else {
                            Log.e("Register", "Error: usuario es null tras registro exitoso.")
                        }
                    } else {
                        // Manejo de error en registro
                    }
                }

        }

        redLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun uploadProfileImage(
        uri: Uri?,
        userId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uri == null) {
            Log.e("RegisterFragment", "URI de imagen es null.")
            onError("No se seleccionó ninguna imagen.")
            return
        }

        val storageRef = FirebaseStorage.getInstance()
            .reference.child("profile_images/$userId.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                Log.d("RegisterFragment", "Imagen subida correctamente")
                storageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        Log.d("RegisterFragment", "URL de descarga: $downloadUri")
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { e ->
                        Log.e("RegisterFragment", "Error obteniendo URL: ${e.message}")
                        onError("Error obteniendo URL: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RegisterFragment", "Error al subir imagen: ${e.message}")
                onError("Error al subir imagen: ${e.message}")
            }
    }

    private fun saveUserToFirestore(uid: String, userMap: HashMap<String, String>) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                progressBar.visibility = View.GONE
            }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Tomar foto", "Elegir de galería")

        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona una opción")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }.show()
    }

    private fun openCamera() {
        val permissions = mutableListOf<String>()
        if (requireContext().checkSelfPermission(android.Manifest.permission.CAMERA)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(android.Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            pendingAction = "camera"
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            launchCamera()
        }
    }

    private fun openGallery() {
        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (requireContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            pendingAction = "gallery"
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            launchGallery()
        }
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "profile_picture.jpg")
        imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )

        Log.d("RegisterFragment", "URI generado para cámara: $imageUri")
        Log.d("RegisterFragment", "Ruta física del archivo: ${photoFile.absolutePath}")
        Log.d("RegisterFragment", "¿Existe archivo antes de abrir cámara?: ${photoFile.exists()} - Tamaño: ${photoFile.length()} bytes")

        cameraLauncher.launch(imageUri)
    }

    private fun launchGallery() {
        galleryLauncher.launch("image/*")
    }
}
