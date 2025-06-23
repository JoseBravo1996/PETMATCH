package com.example.petmatch.ui.detail

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.petmatch.MainActivity
import com.example.petmatch.R
import com.example.petmatch.databinding.FragmentDetailBinding
import com.example.petmatch.domain.model.Pet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class DetailFragment : Fragment(R.layout.fragment_detail) {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentPet: Pet

    // Buffer para bitmap y título si necesitamos pedir permiso de escritura
    private var pendingBitmap: Bitmap? = null
    private var pendingTitle: String? = null

    // Launcher para WRITE_EXTERNAL_STORAGE (Android < Q)
    private val requestWritePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingBitmap?.let { bmp ->
                pendingTitle?.let { title ->
                    saveImageToGallery(bmp, title)
                }
            }
            pendingBitmap = null
            pendingTitle = null
        } else {
            requireContext().toast(getString(R.string.permission_denied))
        }
    }

    @SuppressLint("StringFormatInvalid")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailBinding.bind(view)

        val petId = arguments?.getString("petId")
            ?: return showError(getString(R.string.invalid_pet_id))

        db.collection("pets").document(petId)
            .get()
            .addOnSuccessListener { snap ->
                val pet = snap.toObject(Pet::class.java)
                if (pet == null) return@addOnSuccessListener showError(
                    getString(R.string.pet_not_found)
                )
                currentPet = pet.copy(id = snap.id)
                bindPet(currentPet)
            }
            .addOnFailureListener { e ->
                showError(getString(R.string.error_loading_detail, e.message))
            }

        binding.btnDownload.setOnClickListener {
            downloadPhoto(currentPet.imageUrl, currentPet.name)
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun bindPet(pet: Pet) {
        binding.tvDetailName.text = pet.name
        binding.tvDetailDescription.text = pet.description
        binding.tvDetailTimestamp.text = getString(
            R.string.uploaded_at,
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(pet.timestamp))
        )

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync { map ->
            val pos = LatLng(pet.latitude, pet.longitude)
            map.addMarker(MarkerOptions().position(pos).title(pet.name))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
        }
    }

    private fun downloadPhoto(url: String, title: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) {
                Glide.with(requireContext())
                    .asBitmap()
                    .load(url)
                    .submit()
                    .get()
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingBitmap = bitmap
                pendingTitle = title
                requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                saveImageToGallery(bitmap, title)
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap, title: String) {
        val filename = "${title}_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: run {
            requireContext().toast(getString(R.string.error_saving))
            return
        }

        resolver.openOutputStream(uri)?.use { stream: OutputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        notifyDownloadSuccess(title)
        requireContext().toast(getString(R.string.photo_saved))
    }

    private fun notifyDownloadSuccess(name: String) {
        // Verificar permiso para notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // No tiene permiso, simplemente retornamos
            return
        }

        val notification = NotificationCompat.Builder(
            requireContext(), "download_channel"
        )
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(getString(R.string.photo_saved))
            .setContentText(name)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(requireContext())
            .notify(name.hashCode(), notification)
    }

    private fun showError(msg: String) {
        binding.tvDetailName.text = msg
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Toast helper
    private fun Context.toast(text: String) {
        android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_SHORT).show()
    }
}
