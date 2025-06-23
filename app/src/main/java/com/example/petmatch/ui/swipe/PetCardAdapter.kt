package com.example.petmatch.ui.swipe

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.petmatch.databinding.ItemPetCardBinding
import com.example.petmatch.domain.model.Pet

class PetCardAdapter(
    private val onLike: (Pet) -> Unit,
    private val onItemClick: (Pet) -> Unit
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
            binding.tvName.text = pet.name
         //   binding.tvDescription.text = pet.description
            Glide.with(binding.root)
                .load(pet.imageUrl)
                .into(binding.ivPet)

            // Cuando lo deslicen a la derecha, lo marcaremos como “like”
            binding.root.setOnTouchListener { _, event ->
                // Lo haremos más adelante en el SwipeFragment con el listener de CardStackLayoutManager
                false
            }

            binding.root.setOnClickListener {
                onItemClick(pet)
            }
        }
    }
}