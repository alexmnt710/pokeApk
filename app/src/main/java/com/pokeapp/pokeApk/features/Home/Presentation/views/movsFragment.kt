package com.pokeapp.pokeApk.features.Home.Presentation.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.pokeapp.R
import com.pokeapp.pokeApk.data.localDatabase.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MovsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout del fragmento
        return inflater.inflate(R.layout.fragment_movs, container, false)
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
                                        NavOptions.Builder().setPopUpTo(R.id.movsFragment, true)
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
                            R.id.item_farm -> {
                                findNavController().navigate(R.id.farmFragment)
                                true
                            }

                            else -> false
                        }
                    }
                    popupMenu.show()
                }

            }
        }
    }
}



