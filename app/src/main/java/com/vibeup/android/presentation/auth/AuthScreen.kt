package com.vibeup.android.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vibeup.android.Screen
import com.vibeup.android.ui.theme.DarkBackground
import com.vibeup.android.ui.theme.DarkCard
import com.vibeup.android.ui.theme.TextSecondary
import com.vibeup.android.ui.theme.VibeUpGreen

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Navigate to home on success
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
            viewModel.resetState()
        }
        if (authState is AuthState.Error) {
            errorMessage = (authState as AuthState.Error).message
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Text(
                text = "🎵",
                fontSize = 72.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "VibeUp",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = VibeUpGreen
            )
            Text(
                text = "Your Music, Your Vibe",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Toggle Login/Register
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = {
                        isLoginMode = true
                        errorMessage = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLoginMode) VibeUpGreen
                        else Color.Transparent,
                        contentColor = if (isLoginMode) Color.Black
                        else TextSecondary
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Login", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        isLoginMode = false
                        errorMessage = ""
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLoginMode) VibeUpGreen
                        else Color.Transparent,
                        contentColor = if (!isLoginMode) Color.Black
                        else TextSecondary
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Register", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                placeholder = { Text("Email", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeUpGreen,
                    unfocusedBorderColor = DarkCard,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = VibeUpGreen
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                placeholder = { Text("Password", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None
                else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibeUpGreen,
                    unfocusedBorderColor = DarkCard,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = VibeUpGreen
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                singleLine = true
            )

            // Confirm Password (Register only)
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = ""
                    },
                    placeholder = {
                        Text("Confirm Password", color = TextSecondary)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VibeUpGreen,
                        unfocusedBorderColor = DarkCard,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = VibeUpGreen
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    singleLine = true
                )
            }

            // Error Message
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill all fields!"
                        return@Button
                    }
                    if (!isLoginMode && password != confirmPassword) {
                        errorMessage = "Passwords don't match!"
                        return@Button
                    }
                    if (isLoginMode) viewModel.login(email, password)
                    else viewModel.register(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibeUpGreen,
                    contentColor = Color.Black
                ),
                enabled = authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "Login" else "Register",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip for now
            TextButton(onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }) {
                Text(
                    text = "Skip for now",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
    }
}