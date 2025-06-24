package com.example.petmatch.ui.addpet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.petmatch.R
import com.example.petmatch.databinding.FragmentAddPetBinding
import com.google.android.gms.location.*
import com.google.android.material.chip.Chip
import com.google.android.gms.tasks.CancellationTokenSource

class AddPetFragment : Fragment(R.layout.fragment_add_pet) {

    private var _binding: FragmentAddPetBinding? = null
    private val binding get() = _binding!!
    private val vm: AddPetViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var imageUri: Uri? = null

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLastLocation()
        else Toast.makeText(requireContext(), R.string.permission_denied_location, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("StringFormatInvalid")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddPetBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Chip tags
        val tags = listOf("Cariñoso", "Juguetón", "Tranquilo", "Pequeño", "Grande", "Cachorro", "Adulto", "Sociable")
        tags.forEach { tag ->
            binding.chipGroupTags.addView(Chip(requireContext()).apply {
                text = tag; isCheckable = true
            })
        }
        // Spinners
        binding.spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Perro","Gato","Ave","Otro"))
        binding.spinnerSex.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listOf("Macho","Hembra"))

        binding.btnSelectImage.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }, REQUEST_IMAGE)
        }
        binding.btnSelectLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation()
            } else {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        binding.btnSubmitPet.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val desc = binding.etDescription.text.toString().trim()
            val age = binding.etAge.text.toString().trim().toIntOrNull() ?: 0
            val type = binding.spinnerType.selectedItem?.toString() ?: ""
            val breed = if (binding.switchBreed.isChecked) "Sí" else "No"
            val sex = binding.spinnerSex.selectedItem?.toString() ?: ""
            val isVaccinated = binding.switchVaccinated.isChecked
            val isSterilized = binding.switchSterilized.isChecked
            val selectedTags = (0 until binding.chipGroupTags.childCount)
                .mapNotNull { i -> (binding.chipGroupTags.getChildAt(i) as? Chip)?.takeIf { it.isChecked }?.text.toString() }
            val uri = imageUri
            if (name.isEmpty() || desc.isEmpty() || uri == null) {
                Toast.makeText(requireContext(), R.string.complete_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.submitPet(name, desc, uri, currentLocation?.latitude ?: 0.0, currentLocation?.longitude ?: 0.0, age, type, breed, sex, isVaccinated, isSterilized, selectedTags)
        }

        vm.status.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), R.string.pet_added, Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                },
                onFailure = { e ->
                    Toast.makeText(requireContext(), getString(R.string.error_saving_pet, e.message), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastLocation() {
        val locMgr = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locMgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "Por favor, activa el GPS para obtener la ubicación", Toast.LENGTH_LONG).show()
            return
        }
        // Intenta obtener la última ubicación almacenada
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) updateLocation(loc)
                else getFreshLocation()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), R.string.error_getting_location, Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun getFreshLocation() {
        // Usa getCurrentLocation para una sola petición rápida
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            CancellationTokenSource().token
        ).addOnSuccessListener { loc ->
            if (loc != null) updateLocation(loc)
            else Toast.makeText(requireContext(), R.string.unable_to_get_location, Toast.LENGTH_SHORT).show()
        }
        // Opcional: fallback con requestLocationUpdates
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdates(1)
            .build()
        fusedLocationClient.requestLocationUpdates(req,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { updateLocation(it) }
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }, Looper.getMainLooper()
        )
    }

    private fun updateLocation(loc: Location) {
        currentLocation = loc
        binding.tvLocation.text = getString(R.string.location_set, loc.latitude, loc.longitude)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE && data?.data != null) {
            imageUri = data.data!!
            binding.ivPreview.setImageURI(imageUri)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_IMAGE = 1001
    }
}
