package com.example.petmatch.ui.addpet

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.petmatch.R
import com.example.petmatch.databinding.FragmentAddPetBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class AddPetFragment : Fragment(R.layout.fragment_add_pet) {

    private var _binding: FragmentAddPetBinding? = null
    private val binding get() = _binding!!
    private val vm: AddPetViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var imageUri: Uri? = null

    // Permission launcher for location
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchLastLocation()
        } else {
            Toast.makeText(requireContext(), R.string.permission_denied_location, Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("StringFormatInvalid")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddPetBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Select image
        binding.btnSelectImage.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" },
                REQUEST_IMAGE
            )
        }

        // Request location
        binding.btnSelectLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fetchLastLocation()
            } else {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        // Submit pet
        binding.btnSubmitPet.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val desc = binding.etDescription.text.toString().trim()
            val uri = imageUri
            if (name.isEmpty() || desc.isEmpty() || uri == null) {
                Toast.makeText(requireContext(), R.string.complete_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val lat = currentLocation?.latitude ?: 0.0
            val lng = currentLocation?.longitude ?: 0.0
            vm.submitPet(name, desc, uri, lat, lng)
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

    @SuppressLint("StringFormatInvalid")
    private fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Should not happen as we check before, but guard
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    currentLocation = loc
                    binding.tvLocation.text = getString(
                        R.string.location_set,
                        loc.latitude,
                        loc.longitude
                    )
                } else {
                    Toast.makeText(requireContext(), R.string.unable_to_get_location, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), R.string.error_getting_location, Toast.LENGTH_SHORT).show()
            }
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
