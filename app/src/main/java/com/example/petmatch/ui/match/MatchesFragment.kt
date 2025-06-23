package com.example.petmatch.ui.match

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.petmatch.MainActivity
import com.example.petmatch.R
import com.example.petmatch.databinding.FragmentMatchesBinding
import com.example.petmatch.domain.model.Pet

class MatchesFragment : Fragment(R.layout.fragment_matches) {

    private var _binding: FragmentMatchesBinding? = null
    private val binding get() = _binding!!
    private val vm: MatchesViewModel by viewModels()

    // Launcher para solicitar permiso de notificaciones (Android 13+)
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Solo envía si realmente tienes el permiso
            pendingNotifications.forEach { pet ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    sendMatchNotification(pet)
                }
            }
            pendingNotifications.clear()
        }
    }
    private val pendingNotifications = mutableListOf<Pet>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMatchesBinding.bind(view)

        val prefs = requireContext()
            .getSharedPreferences("petmatch_prefs", Context.MODE_PRIVATE)
        val seenKey = "seen_matches"

        // Inicializa RecyclerView
        binding.rvMatches.layoutManager = LinearLayoutManager(requireContext())

        vm.matches.observe(viewLifecycleOwner) { list ->
            // 1) Cargar set de vistos
            val seen = prefs.getStringSet(seenKey, emptySet())!!.toMutableSet()

            // 2) Detectar nuevos
            val newMatches = list.filter { it.id !in seen }

            // 3) Notificar y marcar como vistos
            newMatches.forEach { pet ->
                // Si Android 13+, asegurar permiso; si no, enviar directo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Guardar pendiente y pedir permiso
                    pendingNotifications += pet
                    requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    sendMatchNotification(pet)
                }
                seen.add(pet.id)
            }

            // 4) Guardar vistos
            prefs.edit()
                .putStringSet(seenKey, seen)
                .apply()

            // 5) Mostrar lista completa
            binding.rvMatches.adapter = PetListAdapter(list) { pet ->
                findNavController().navigate(
                    R.id.detailFragment,
                    bundleOf("petId" to pet.id)
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendMatchNotification(pet: Pet) {
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("petId", pet.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            pet.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(requireContext(), "match_channel")
            .setSmallIcon(R.drawable.ic_favorite)
            .setContentTitle("¡Nuevo match mutuo!")
            .setContentText("Has hecho match con ${pet.name}")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(requireContext())
            .notify(pet.id.hashCode(), notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}