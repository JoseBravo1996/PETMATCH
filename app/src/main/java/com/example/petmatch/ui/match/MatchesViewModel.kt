package com.example.petmatch.ui.match

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petmatch.data.repository.PetRepository
import com.example.petmatch.domain.model.Pet
import com.google.firebase.auth.FirebaseAuth

class MatchesViewModel : ViewModel() {
    private val repo = PetRepository()
    private val _matches = MutableLiveData<List<Pet>>()
    val matches: LiveData<List<Pet>> = _matches

    init {
        loadMutualMatches()
    }

    private fun loadMutualMatches() {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        repo.getMutualMatches(uid) { list ->
            _matches.postValue(list)
        }
    }

    fun onLike(pet: Pet) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        repo.likePet(uid, pet) { success, error ->
            if (!success) {
                Log.e("SwipeVM", "Error guardando like: $error")
            }
        }
    }
}