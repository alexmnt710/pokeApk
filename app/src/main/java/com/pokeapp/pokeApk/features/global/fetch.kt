package com.pokeapp.pokeApk.features.global
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun obtenerNombreDeUsuario(): String? {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    if (uid != null) {
        val documento = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()
        return documento.getString("username")
    }
    return null
}
suspend fun obtenerIdToken(): String? {
    val user = FirebaseAuth.getInstance().currentUser
    return user?.getIdToken(true)?.await()?.token
}

