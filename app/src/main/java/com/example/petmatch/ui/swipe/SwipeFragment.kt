package com.example.petmatch.ui.swipe

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.petmatch.R
import com.example.petmatch.databinding.FragmentSwipeBinding
import com.google.android.material.chip.Chip
import com.yuyakaido.android.cardstackview.*
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.petmatch.databinding.BottomSheetFiltersBinding
import androidx.fragment.app.activityViewModels
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class SwipeFragment : Fragment(R.layout.fragment_swipe) {
    private var _binding: FragmentSwipeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SwipeViewModel by activityViewModels()
    private lateinit var manager: CardStackLayoutManager
    private lateinit var adapter: PetCardAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSwipeBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        // Obtener ubicación actual y pasarla al ViewModel
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        viewModel.setUserLocation(loc.latitude, loc.longitude)
                    }
                }
        } catch (e: SecurityException) {
            // No hay permisos, ignorar
        }

        // Bloqueo: si NO tiene mascota, redirigir a alta
        val repo = com.example.petmatch.data.repository.PetRepository()
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        repo.getUserPets(uid) { pets ->
            if (pets.isEmpty()) {
                requireActivity().runOnUiThread {
                    android.widget.Toast.makeText(requireContext(), "Debes registrar tu mascota para ver otras", android.widget.Toast.LENGTH_LONG).show()
                    findNavController().navigate(com.example.petmatch.R.id.addPetFragment)
                }
                return@getUserPets
            }
        }

        manager = CardStackLayoutManager(requireContext(), object : CardStackListener {
            override fun onCardSwiped(direction: Direction?) {
                if (direction == Direction.Right) {
                    val index = manager.topPosition - 1
                    if (index in adapter.currentList.indices) {
                        val pet = adapter.currentList[index]
                        viewModel.onLike(pet)
                    } else {
                        Log.d("SwipeDebug", "Index $index fuera de rango")
                    }
                }
            }
            // Otros callbacks vacíos
            override fun onCardDragging(direction: Direction?, ratio: Float) {}
            override fun onCardRewound() {}
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: View?, position: Int) {}
            override fun onCardDisappeared(view: View?, position: Int) {}
        })
        manager.setStackFrom(StackFrom.None)

        adapter = PetCardAdapter(
            onLike = { pet ->
                viewModel.onLike(pet)
            },
            onItemClick = { pet ->
                Log.d("SwipeFragment", "Navegando a Detail con pet.id='${pet.id}'")
                // Aquí navegamos al DetailFragment, pasando el pet.id
                findNavController().navigate(
                    R.id.detailFragment,
                    bundleOf("petId" to pet.id)
                )
            }
        )

        binding.cardStackView.layoutManager = manager
        binding.cardStackView.adapter = adapter

        // 2) Poblar chips cuando cambie la lista base de pets
        viewModel.allPets.observe(viewLifecycleOwner) { pets ->
            // Extraer etiquetas únicas
            val tags = pets.flatMap { it.tags }.toSet().sorted()
            binding.chipGroupTags.removeAllViews()
            tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = "#$tag"
                    isCheckable = true
                }
                binding.chipGroupTags.addView(chip)
            }
        }

        // 3) Escuchar selección de chips
        binding.chipGroupTags.setOnCheckedStateChangeListener { group, checkedIds ->
            val selected = checkedIds.mapNotNull { id ->
                (group.findViewById<Chip>(id).text.toString().removePrefix("#"))
            }.toSet()
            viewModel.setSelectedTags(selected)
        }

        viewModel.pets.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            // Mostrar Toast con info de filtros y cantidad
            val sexFiltro = viewModel.myPet.value?.sex ?: "?"
            val filtroSexo = viewModel.myPet.value?.sex ?: "?"
            val filtroEdadMin = viewModel.pets.value?.let { _ -> viewModel.pets.value } ?: ""
            val filtroEdadMax = viewModel.pets.value?.let { _ -> viewModel.pets.value } ?: ""
            val filtroDist = viewModel.pets.value?.let { _ -> viewModel.pets.value } ?: ""
            Toast.makeText(requireContext(), "Mascotas: ${list.size} | Sexo: $sexFiltro | Edad: $filtroEdadMin-$filtroEdadMax | Dist: $filtroDist km", Toast.LENGTH_LONG).show()

            // Agregar logs detallados
            android.util.Log.d("SwipeFiltro", "Mascotas filtradas: ${list.size}")
            android.util.Log.d("SwipeFiltro", "Sexo filtro: $sexFiltro")
            android.util.Log.d("SwipeFiltro", "Edad filtro: $filtroEdadMin-$filtroEdadMax")
            android.util.Log.d("SwipeFiltro", "Distancia filtro: $filtroDist km")
            android.util.Log.d("SwipeFiltro", "IDs mascotas: ${list.map { it.id }}")
        }

        // Botón de filtro
        binding.btnFilter.setOnClickListener {
            FiltersBottomSheetFragment().show(parentFragmentManager, "FiltersBottomSheet")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// BottomSheetDialogFragment para filtros
class FiltersBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetFiltersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SwipeViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cerrar
        binding.btnClose.setOnClickListener { dismiss() }

        // Sexo
        binding.rgSexo.check(
            when (viewModel.myPet.value?.sex) {
                "Macho" -> binding.rbHembra.id // por defecto mostrar el opuesto
                "Hembra" -> binding.rbMacho.id
                else -> binding.rbAmbos.id
            }
        )

        // Edad y distancia (puedes guardar los valores actuales en el ViewModel si lo deseas)

        binding.btnAplicarFiltros.setOnClickListener {
            // Sexo
            val sex = when (binding.rgSexo.checkedRadioButtonId) {
                binding.rbMacho.id -> "Macho"
                binding.rbHembra.id -> "Hembra"
                else -> null // Ambos
            }
            // Edad
            val edadMin = binding.etEdadMin.text.toString().toIntOrNull()
            val edadMax = binding.etEdadMax.text.toString().toIntOrNull()
            // Distancia
            val distancia = binding.etDistancia.text.toString().toFloatOrNull()

            // LOGS para depuración
            android.util.Log.d("SwipeFiltro", "[BottomSheet] Aplicar filtros: sexo=$sex, edadMin=$edadMin, edadMax=$edadMax, distancia=$distancia")

            viewModel.setFilterSex(sex)
            viewModel.setFilterEdad(edadMin, edadMax)
            viewModel.setFilterDistancia(distancia)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}