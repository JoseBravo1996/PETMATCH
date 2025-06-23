package com.example.petmatch.data.repository

import android.net.Uri
import com.example.petmatch.domain.model.Pet
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class PetRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance().reference
    private val petsCol = db.collection("pets")
    private val likesCol = db.collection("likes")

    fun getUserLikes(
        userId: String,
        onUpdate: (Set<String>) -> Unit
    ) {
        likesCol.document(userId)
            .collection("userLikes")
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                onUpdate(ids)
            }
    }

    fun getPetsByIds(
        ids: List<String>,
        onComplete: (List<Pet>) -> Unit
    ) {
        if (ids.isEmpty()) {
            onComplete(emptyList())
            return
        }
        // Firestore admite hasta 10 valores en whereIn
        petsCol.whereIn(FieldPath.documentId(), ids)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    doc.toObject(Pet::class.java)?.copy(id = doc.id)
                }
                onComplete(list)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }


    fun addPet(
        ownerId: String,
        name: String,
        description: String,
        imageUri: Uri,
        latitude: Double,
        longitude: Double,
        onComplete: (Boolean, String?) -> Unit
    ) {
        // Genera un ID de mascota
        val petId = db.collection("pets").document().id
        // Ruta en Storage
        val imageRef = storage.child("pet_images/$ownerId/$petId.jpg")

        // 1) Subir la imagen
        imageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                imageRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                // 2) Guardar el documento
                val pet = Pet(
                    id = petId,
                    ownerId = ownerId,
                    name = name,
                    description = description,
                    imageUrl = uri.toString(),
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = System.currentTimeMillis()
                )
                db.collection("pets").document(petId)
                    .set(pet)
                    .addOnSuccessListener { onComplete(true, null) }
                    .addOnFailureListener { e -> onComplete(false, e.message) }
            }
            .addOnFailureListener { e ->
                onComplete(false, e.message)
            }
    }

    fun getAllOtherPets(
        currentUserId: String,
        onUpdate: (List<Pet>) -> Unit
    ) {
        // Firestore soporta != a partir de la versión 24.0.0 del BOM
        petsCol
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener

                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        // Convierte el documento a Pet y copia el ID real
                        doc.toObject(Pet::class.java)
                            ?.copy(id = doc.id)
                    }
                    // Excluye tus propias mascotas
                    ?.filter { it.ownerId != currentUserId }
                    ?: emptyList()

                onUpdate(list)
            }
    }

    fun getAllPets(onUpdate: (List<Pet>) -> Unit) {
        petsCol
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener

                val list = snap?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Pet::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                onUpdate(list)
            }
    }

    /** Guarda un like de userId sobre pet.id */
    fun likePet(userId: String, pet: Pet, onComplete: (Boolean, String?) -> Unit = {_,_->}) {
        val likeDoc = db.collection("likes")
            .document(userId)
            .collection("userLikes")
            .document(pet.id)

        likeDoc.set(mapOf("likedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { onComplete(true, null) }
            .addOnFailureListener { e -> onComplete(false, e.message) }
    }

    fun getUserPets(
        userId: String,
        onComplete: (List<Pet>) -> Unit
    ) {
        db.collection("pets")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull { doc ->
                    doc.toObject(Pet::class.java)?.copy(id = doc.id)
                }
                onComplete(list)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    fun getMutualMatches(
        userId: String,
        onComplete: (List<Pet>) -> Unit
    ) {
        // 1) Obtengo lo que yo likeé (IDs de pets)
        getUserLikes(userId) { likedIds ->
            if (likedIds.isEmpty()) return@getUserLikes onComplete(emptyList())

            // 2) Cargo esos Pets y extraigo sus ownerId
            getPetsByIds(likedIds.toList()) { likedPets ->
                val otherOwners = likedPets.map { it.ownerId }.distinct()

                // 3) Obtengo mis mascotas
                getUserPets(userId) { myPets ->
                    val myPetIds = myPets.map { it.id }.toSet()

                    // 4) Para cada owner en otherOwners, compruebo si me likeó alguna de mis pets
                    val mutualPets = mutableListOf<Pet>()
                    val repo = this

                    // Llevamos la cuenta de cuántos dueños hemos chequeado
                    var remaining = otherOwners.size
                    if (remaining == 0) return@getUserPets onComplete(emptyList())

                    otherOwners.forEach { ownerId ->
                        // Reviso la colección likes/<ownerId>/userLikes
                        db.collection("likes")
                            .document(ownerId)
                            .collection("userLikes")
                            .get()
                            .addOnSuccessListener { snap ->
                                val theirLikes = snap.documents.map { it.id }.toSet()
                                // Si me likearon al menos una de mis pets, entonces todos los pets de este owner que yo likeé son mutuos
                                if (theirLikes.intersect(myPetIds).isNotEmpty()) {
                                    // Añadimos esas mascotas
                                    mutualPets += likedPets.filter { it.ownerId == ownerId }
                                }
                                if (--remaining == 0) {
                                    onComplete(mutualPets)
                                }
                            }
                            .addOnFailureListener {
                                if (--remaining == 0) {
                                    onComplete(mutualPets)
                                }
                            }
                    }
                }
            }
        }
    }
}