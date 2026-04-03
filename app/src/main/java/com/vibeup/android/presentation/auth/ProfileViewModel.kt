package com.vibeup.android.presentation.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _displayName = MutableStateFlow(
        firebaseAuth.currentUser?.displayName ?: ""
    )
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _photoUrl = MutableStateFlow(
        firebaseAuth.currentUser?.photoUrl?.toString() ?: ""
    )
    val photoUrl: StateFlow<String> = _photoUrl.asStateFlow()

    val email = firebaseAuth.currentUser?.email ?: ""

    // ✅ Load profile on init
    init {
        loadProfileFromFirestore()
    }

    private fun loadProfileFromFirestore() {
        viewModelScope.launch {
            try {
                val uid = firebaseAuth.currentUser?.uid ?: return@launch
                val doc = firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()

                // Load nickname
                doc.getString("nickname")?.let {
                    _displayName.value = it
                }

                // Load photo URL from Firestore if not in Auth
                if (_photoUrl.value.isEmpty()) {
                    doc.getString("photoUrl")?.let {
                        _photoUrl.value = it
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileVM", "Load failed: ${e.message}")
            }
        }
    }

    fun updateProfile(nickname: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val profileUpdates = userProfileChangeRequest {
                    displayName = nickname
                }
                firebaseAuth.currentUser
                    ?.updateProfile(profileUpdates)
                    ?.await()
                _displayName.value = nickname

                // Save to Firestore
                firebaseAuth.currentUser?.uid?.let { uid ->
                    firestore.collection("users").document(uid)
                        .set(mapOf(
                            "nickname" to nickname,
                            "email" to email
                        )).await()
                }
                _profileState.value = ProfileState.Success(
                    "Profile updated! ✅"
                )
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(
                    e.message ?: "Update failed"
                )
            }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) {
                    _profileState.value = ProfileState.Error(
                        "Not logged in!"
                    )
                    return@launch
                }

                android.util.Log.d("ProfileVM", "Uploading for uid: $uid")

                // ✅ Correct path
                val ref = storage.reference
                    .child("profiles")
                    .child("$uid.jpg")

                // Upload file
                ref.putFile(uri).await()
                android.util.Log.d("ProfileVM", "Upload successful!")

                // Get download URL
                val downloadUrl = ref.downloadUrl.await()
                android.util.Log.d(
                    "ProfileVM",
                    "Download URL: $downloadUrl"
                )

                // Update Firebase Auth profile
                val profileUpdates = userProfileChangeRequest {
                    photoUri = downloadUrl
                }
                firebaseAuth.currentUser
                    ?.updateProfile(profileUpdates)
                    ?.await()

                // Update Firestore
                firestore.collection("users")
                    .document(uid)
                    .update("photoUrl", downloadUrl.toString())
                    .await()

                _photoUrl.value = downloadUrl.toString()
                _profileState.value = ProfileState.Success(
                    "Profile picture updated! ✅"
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "ProfileVM",
                    "Upload failed: ${e.message}"
                )
                _profileState.value = ProfileState.Error(
                    "Upload failed: ${e.message}"
                )
            }
        }
    }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                firebaseAuth.sendPasswordResetEmail(email).await()
                _profileState.value = ProfileState.Success(
                    "Password reset email sent to $email"
                )
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(
                    e.message ?: "Failed to send email"
                )
            }
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val message: String) : ProfileState()
    data class Error(val message: String) : ProfileState()
}