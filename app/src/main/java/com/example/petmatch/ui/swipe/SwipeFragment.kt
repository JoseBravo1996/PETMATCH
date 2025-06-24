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

class SwipeFragment : Fragment(R.layout.fragment_swipe) {
    private var _binding: FragmentSwipeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SwipeViewModel by viewModels()
    private lateinit var manager: CardStackLayoutManager
    private lateinit var adapter: PetCardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSwipeBinding.bind(view)

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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}