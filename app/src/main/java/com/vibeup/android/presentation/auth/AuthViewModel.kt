package com.vibeup.android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = firebaseAuth.currentUser
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                _currentUser.value = firebaseAuth.currentUser
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun continueAsGuest() {
        // Simply mark as "logged in" without Firebase — skip mode
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                firebaseAuth.signInAnonymously().await()
                _currentUser.value = firebaseAuth.currentUser
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                // If anonymous auth fails, just set a dummy state to navigate
                _authState.value = AuthState.Guest
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                if (email.isBlank()) {
                    _authState.value = AuthState.Error("Enter your email first")
                    return@launch
                }
                firebaseAuth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.ResetSent
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }
}

sealed class AuthState {
    object Idle      : AuthState()
    object Loading   : AuthState()
    object Success   : AuthState()
    object Guest     : AuthState()   // 👈 ADD
    object ResetSent : AuthState()   // 👈 ADD
    data class Error(val message: String) : AuthState()
}