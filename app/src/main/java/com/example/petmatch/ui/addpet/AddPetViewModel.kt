package com.example.petmatch.ui.addpet

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.petmatch.data.repository.PetRepository
import com.google.firebase.auth.FirebaseAuth

class AddPetViewModel : ViewModel() {
    private val repo = PetRepository()
    private val _status = MutableLiveData<Result<Unit>>()
    val status: LiveData<Result<Unit>> = _status

    fun submitPet(
        name: String,
        description: String,
        imageUri: Uri,
        latitude: Double,
        longitude: Double,
        age: Int,
        type: String,
        breed: String,
        sex: String,
        vaccinated: Boolean,
        sterilized: Boolean,
        tags: List<String>
    ) {
        val uid = FirebaseAuth.getInstance().currentUser!!.uid
        repo.addPet(uid, name, description, imageUri, latitude, longitude, age, type, breed, sex, vaccinated, sterilized, tags) { success, error ->
            if (success) _status.postValue(Result.success(Unit))
            else _status.postValue(Result.failure(Exception(error)))
        }
    }
}