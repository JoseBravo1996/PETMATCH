package com.example.petmatch.ui.swipe

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petmatch.databinding.ItemPetCardBinding
import com.example.petmatch.domain.model.Pet
import java.util.Locale

class PetCardAdapter(
    private val onLike: (Pet) -> Unit,
    private val onItemClick: (Pet) -> Unit,
    private val userLat: Double? = null,
    private val userLng: Double? = null
) : RecyclerView.Adapter<PetCardAdapter.PetViewHolder>() {

    private val items = mutableListOf<Pet>()

    val currentList: List<Pet>
        get() = items

    fun submitList(list: List<Pet>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class PetViewHolder(private val binding: ItemPetCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: Pet) {
            // Nombre y edad sobre la foto
            binding.tvName.text = pet.name
            binding.tvAge.text = if (pet.age > 0) ", ${pet.age} años" else ""
            Glide.with(binding.root)
                .load(pet.imageUrl)
                .into(binding.ivPet)

            // Info panel inferior
            binding.tvType.text = pet.type
            binding.tvBreed.text = pet.breed
            binding.tvSex.text = pet.sex
            binding.chipVaccinated.visibility = if (pet.vaccinated) View.VISIBLE else View.GONE
            binding.chipSterilized.visibility = if (pet.sterilized) View.VISIBLE else View.GONE

            // Ubicación (ciudad con Geocoder)
            if (pet.latitude != 0.0 && pet.longitude != 0.0) {
                val context: Context = binding.root.context
                val geocoder = Geocoder(context, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(pet.latitude, pet.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val city = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: ""
                        binding.tvLocation.text = if (city.isNotBlank()) city else String.format("Lat: %.3f, Lon: %.3f", pet.latitude, pet.longitude)
                    } else {
                        binding.tvLocation.text = String.format("Lat: %.3f, Lon: %.3f", pet.latitude, pet.longitude)
                    }
                } catch (e: Exception) {
                    binding.tvLocation.text = String.format("Lat: %.3f, Lon: %.3f", pet.latitude, pet.longitude)
                }
            } else {
                binding.tvLocation.text = "Ubicación desconocida"
            }

            // Distancia real
            if (userLat != null && userLng != null && pet.latitude != 0.0 && pet.longitude != 0.0) {
                val results = FloatArray(1)
                Location.distanceBetween(userLat, userLng, pet.latitude, pet.longitude, results)
                val km = results[0] / 1000f
                binding.tvDistance.text = String.format("~%.1f km de ti", km)
            } else {
                binding.tvDistance.text = "~? km de ti"
            }

            // Chips de tags
            binding.chipGroupTags.removeAllViews()
            pet.tags.filter { it.isNotBlank() && it != "null" }.forEach { tag ->
                val chip = com.google.android.material.chip.Chip(binding.root.context).apply {
                    text = tag
                    isClickable = false
                    isCheckable = false
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge)
                }
                binding.chipGroupTags.addView(chip)
            }

            // Descripción
            binding.tvDescription.text = pet.description

            binding.root.setOnTouchListener { _, _ -> false }
            binding.root.setOnClickListener { onItemClick(pet) }
        }
    }
}