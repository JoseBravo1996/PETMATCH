package com.example.petmatch.ui.swipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petmatch.data.repository.PetRepository
import com.example.petmatch.domain.model.Pet
import com.google.firebase.auth.FirebaseAuth

class SwipeViewModel : ViewModel() {
    private val repo = PetRepository()
    private val uid = FirebaseAuth.getInstance().currentUser!!.uid

    private val _allPets = MutableLiveData<List<Pet>>()
    val allPets: LiveData<List<Pet>> = _allPets
    private val _selectedTags = MutableLiveData<Set<String>>(emptySet())

    private val _likedIds = MutableLiveData<Set<String>>()
    val likedIds: LiveData<Set<String>> = _likedIds

    private val _myPet = MutableLiveData<Pet?>()
    val myPet: LiveData<Pet?> = _myPet

    val pets: LiveData<List<Pet>> = MediatorLiveData<List<Pet>>().apply {
        fun update() {
            val all    = _allPets.value ?: emptyList()
            val liked  = _likedIds.value  ?: emptySet()
            val tags   = _selectedTags.value ?: emptySet()
            val myPet  = _myPet.value

            // 1) Excluir propias y ya likeadas
            var base = all.filter { it.ownerId != uid && it.id !in liked }

            // 2) Filtrar por tipo y sexo respecto a mi mascota
            if (myPet != null) {
                base = base.filter { it.type == myPet.type && it.sex != myPet.sex }
            }

            // 3) Si no hay tags seleccionadas, retorno todo
            value = if (tags.isEmpty()) base
            else base.filter { pet ->
                pet.tags.any { it in tags }
            }
        }
        addSource(_allPets)     { update() }
        addSource(_likedIds)    { update() }
        addSource(_selectedTags){ update() }
        addSource(_myPet)       { update() }
    }

    init {
        // Inicia las escuchas
        repo.getAllOtherPets(uid) { list ->
            _allPets.postValue(list)
        }
        repo.getUserLikes(uid) { setIds ->
            _likedIds.postValue(setIds)
        }
        repo.getUserPets(uid) { pets ->
            _myPet.postValue(pets.firstOrNull())
        }
    }

    fun onLike(pet: Pet) {
        repo.likePet(uid, pet) { success, error ->
            // opcional: manejar error
        }
    }

    fun setSelectedTags(tags: Set<String>) {
        _selectedTags.value = tags
    }
}