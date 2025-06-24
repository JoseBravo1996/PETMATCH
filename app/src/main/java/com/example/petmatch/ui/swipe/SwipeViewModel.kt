package com.example.petmatch.ui.swipe

import android.util.Log
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

    // Filtros adicionales
    private val _filterSex = MutableLiveData<String?>(null) // "Macho", "Hembra", null (ambos)
    private val _filterEdadMin = MutableLiveData<Int?>(null)
    private val _filterEdadMax = MutableLiveData<Int?>(null)
    private val _filterDistancia = MutableLiveData<Float?>(null) // en km
    private val _userLat = MutableLiveData<Double?>(null)
    private val _userLng = MutableLiveData<Double?>(null)

    private val _pets = MediatorLiveData<List<Pet>>().apply {
        fun update() {
            Log.d("SwipeFiltroVM", "update() ejecutado: allPets=${_allPets.value?.size}, likedIds=${_likedIds.value?.size}, tags=${_selectedTags.value}, myPet=${_myPet.value}, filterSex=${_filterSex.value}, edadMin=${_filterEdadMin.value}, edadMax=${_filterEdadMax.value}, distMax=${_filterDistancia.value}, userLat=${_userLat.value}, userLng=${_userLng.value}")
            val all    = _allPets.value ?: emptyList()
            val liked  = _likedIds.value  ?: emptySet()
            val tags   = _selectedTags.value ?: emptySet()
            val myPet  = _myPet.value
            val filterSex = _filterSex.value
            val edadMin = _filterEdadMin.value
            val edadMax = _filterEdadMax.value
            val distMax = _filterDistancia.value
            val userLat = _userLat.value
            val userLng = _userLng.value

            // 1) Excluir propias y ya likeadas
            var base = all.filter { it.ownerId != uid && it.id !in liked }

            // 2) Filtrar solo por tipo igual al de mi mascota
            if (myPet != null) {
                base = base.filter { it.type == myPet.type }
            }

            // 3) Filtro de sexo (del filtro, no del usuario)
            if (filterSex != null && filterSex != "Ambos") {
                base = base.filter { it.sex == filterSex }
            }
            // 4) Filtro de edad
            if (edadMin != null) base = base.filter { it.age >= edadMin }
            if (edadMax != null) base = base.filter { it.age <= edadMax }
            // 5) Filtro de distancia
            if (distMax != null && userLat != null && userLng != null) {
                base = base.filter { pet ->
                    pet.latitude != 0.0 && pet.longitude != 0.0 &&
                    android.location.Location("user").apply {
                        latitude = userLat; longitude = userLng
                    }.distanceTo(android.location.Location("pet").apply {
                        latitude = pet.latitude; longitude = pet.longitude
                    }) / 1000f <= distMax
                }
            }

            // 6) Si no hay tags seleccionadas, retorno todo
            value = if (tags.isEmpty()) base
            else base.filter { pet ->
                pet.tags.any { it in tags }
            }
        }
        addSource(_allPets)     { update() }
        addSource(_likedIds)    { update() }
        addSource(_selectedTags){ update() }
        addSource(_myPet)       { update() }
        addSource(_filterSex)   { update() }
        addSource(_filterEdadMin){ update() }
        addSource(_filterEdadMax){ update() }
        addSource(_filterDistancia){ update() }
        addSource(_userLat){ update() }
        addSource(_userLng){ update() }
    }
    val pets: LiveData<List<Pet>> = _pets

    fun setFilterSex(sex: String?) { _filterSex.value = sex }
    fun setFilterEdad(min: Int?, max: Int?) { _filterEdadMin.value = min; _filterEdadMax.value = max }
    fun setFilterDistancia(dist: Float?) { _filterDistancia.value = dist }
    fun setUserLocation(lat: Double?, lng: Double?) { _userLat.value = lat; _userLng.value = lng }

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