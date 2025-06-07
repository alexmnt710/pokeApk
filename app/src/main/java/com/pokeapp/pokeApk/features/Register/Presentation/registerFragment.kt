package com.pokeapp.pokeApk.features.Register.Presentation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.INPUT_METHOD_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.pokeapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File
import java.io.FileOutputStream


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
    private var isImageSelected = false

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var pendingAction: String = ""

    private lateinit var cloudinaryManager: MediaManager

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
                    isImageSelected = true
                }
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                profileImage.setImageURI(it)
                imageUri = it
                isImageSelected = true
            }
        }


        profileImage.setOnClickListener {
            showImagePickerDialog()
        }

        btnRegister.setOnClickListener {

            hideKeyboard()

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
                        user?.let {
                            if (isImageSelected) {
                                uploadProfileImage(imageUri, user.uid,
                                    onSuccess = { imageUrl ->
                                        val userMap = hashMapOf(
                                            "username" to username,
                                            "email" to email,
                                            "profileImage" to imageUrl
                                        )
                                        saveUserToFirestore(user.uid, userMap)
                                    },
                                    onError = {
                                        progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                                    })
                            } else {
                                val userMap = hashMapOf(
                                    "username" to username,
                                    "email" to email,
                                    "profileImage" to ""
                                )
                                saveUserToFirestore(user.uid, userMap)
                            }
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        redLogin.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun hideKeyboard() {
        // Obtiene la vista actualmente enfocada
        val view = requireActivity().currentFocus
        if (view != null) {
            // Obtén el InputMethodManager a partir del contexto de la actividad
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            // Oculta el teclado a partir del token de la ventana de la vista
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            outputStream.close()
            inputStream?.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uploadProfileImage(
        uri: Uri?,
        userId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (uri == null) {
            Log.e("RegisterFragment", "URI es null")
            onError("URI inválida")
            return
        }

        val file = getFileFromUri(requireContext(), uri)
        if (file == null) {
            Log.e("RegisterFragment", "No se pudo crear archivo desde URI")
            onError("No se pudo preparar la imagen")
            return
        }

        MediaManager.get().upload(file.absolutePath)
            .option("public_id", "profile_images/$userId")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary", "Subida iniciada")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    Log.d("Cloudinary", "Imagen subida: $imageUrl")
                    requireActivity().runOnUiThread {
                        onSuccess(imageUrl ?: "")
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e("Cloudinary", "Error al subir: ${error?.description}")
                    requireActivity().runOnUiThread {
                        onError("Error Cloudinary: ${error?.description}")
                    }
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.w("Cloudinary", "Reagendado: ${error?.description}")
                }
            })
            .dispatch()
    }


    private fun saveUserToFirestore(uid: String, userMap: HashMap<String, String>) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(userMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.loginFragment)
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
