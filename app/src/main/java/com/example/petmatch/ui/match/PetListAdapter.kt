package com.example.petmatch.ui.match

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petmatch.databinding.ItemPetCardBinding
import com.example.petmatch.domain.model.Pet

class PetListAdapter(
    private val items: List<Pet>,
    private val onItemClick: (Pet) -> Unit
) : RecyclerView.Adapter<PetListAdapter.PetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding = ItemPetCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PetViewHolder(private val binding: ItemPetCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pet: Pet) {
            binding.tvName.text = pet.name
         //   binding.tvDescription.text = pet.description
            Glide.with(binding.root)
                .load(pet.imageUrl)
                .into(binding.ivPet)

            binding.root.setOnClickListener {
                onItemClick(pet)
            }
        }
    }
}