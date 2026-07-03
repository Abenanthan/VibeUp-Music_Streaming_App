package com.vibeup.android.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vibeup.android.R
import com.vibeup.android.Screen
import com.vibeup.android.ui.theme.*

@Composable
fun AuthScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }
    var forgotEmail by remember { mutableStateOf("") }

    // ✅ Handle all auth states properly
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success,
            is AuthState.Guest -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                errorMessage = (authState as AuthState.Error).message
            }
            is AuthState.ResetSent -> {
                errorMessage = "✅ Reset email sent! Check your inbox."
            }
            else -> {}
        }
    }

    // Forgot Password Dialog
    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = {
                showForgotPassword = false
                forgotEmail = ""
            },
            title = {
                Text(
                    "Reset Password",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Enter your email to receive a reset link",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it },
                        placeholder = {
                            Text("Email", color = Color(0xFF4B5563))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = Color(0xFF2A2A4A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PurplePrimary,
                            focusedContainerColor = Color(0xFF12122A),
                            unfocusedContainerColor = Color(0xFF12122A)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendPasswordReset(forgotEmail)
                        showForgotPassword = false
                        forgotEmail = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Reset Link")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showForgotPassword = false
                        forgotEmail = ""
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF12122A),
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A))
    ) {
        // Background gradient glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PurplePrimary.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            BluePrimary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ✅ App Logo
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF12122A)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "VibeUp Logo",
                    modifier = Modifier.size(90.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = "VibeUp",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PurpleLight, BlueLight)
                    )
                )
            )
            Text(
                text = "Feel the music 🎧",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // ── Tab Toggle ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D0D2B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Sign In" to true, "Sign Up" to false)
                    .forEach { (label, loginMode) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isLogin == loginMode)
                                        Brush.linearGradient(
                                            colors = listOf(
                                                PurplePrimary,
                                                BluePrimary
                                            )
                                        )
                                    else
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent
                                            )
                                        )
                                )
                                .clickable {
                                    isLogin = loginMode
                                    errorMessage = ""
                                    viewModel.resetState()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLogin == loginMode)
                                    Color.White
                                else
                                    TextSecondary
                            )
                        }
                    }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Email Field ──
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = ""
                },
                label = { Text("Email", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = Color(0xFF2A2A4A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PurplePrimary,
                    focusedContainerColor = Color(0xFF12122A),
                    unfocusedContainerColor = Color(0xFF12122A),
                    focusedLabelColor = PurplePrimary,
                    unfocusedLabelColor = TextSecondary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Password Field ──
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = ""
                },
                label = { Text("Password", color = TextSecondary) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { showPassword = !showPassword }
                    ) {
                        Icon(
                            if (showPassword)
                                Icons.Default.VisibilityOff
                            else
                                Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                visualTransformation = if (showPassword)
                    VisualTransformation.None
                else
                    PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = Color(0xFF2A2A4A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PurplePrimary,
                    focusedContainerColor = Color(0xFF12122A),
                    unfocusedContainerColor = Color(0xFF12122A),
                    focusedLabelColor = PurplePrimary,
                    unfocusedLabelColor = TextSecondary
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isLogin) ImeAction.Done
                    else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isLogin) viewModel.login(email, password)
                    },
                    onNext = {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                )
            )

            // ── Confirm Password (Sign Up only) ──
            AnimatedVisibility(
                visible = !isLogin,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        label = {
                            Text(
                                "Confirm Password",
                                color = TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    showConfirmPassword = !showConfirmPassword
                                }
                            ) {
                                Icon(
                                    if (showConfirmPassword)
                                        Icons.Default.VisibilityOff
                                    else
                                        Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (showConfirmPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurplePrimary,
                            unfocusedBorderColor = Color(0xFF2A2A4A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PurplePrimary,
                            focusedContainerColor = Color(0xFF12122A),
                            unfocusedContainerColor = Color(0xFF12122A),
                            focusedLabelColor = PurplePrimary,
                            unfocusedLabelColor = TextSecondary
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (password == confirmPassword) {
                                    viewModel.register(email, password)
                                } else {
                                    errorMessage = "Passwords don't match!"
                                }
                            }
                        )
                    )
                }
            }

            // ── Forgot Password ──
            if (isLogin) {
                TextButton(
                    onClick = { showForgotPassword = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        "Forgot Password?",
                        color = PurplePrimary,
                        fontSize = 13.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Error Message ──
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (errorMessage.startsWith("✅"))
                                Color(0xFF065F46).copy(alpha = 0.3f)
                            else
                                Color(0xFF7F1D1D).copy(alpha = 0.3f)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = if (errorMessage.startsWith("✅"))
                            Color(0xFF10B981)
                        else
                            Color(0xFFFC8181),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Main Action Button ──
            val isLoading = authState is AuthState.Loading

            Button(
                onClick = {
                    errorMessage = ""
                    focusManager.clearFocus()
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please fill in all fields!"
                        return@Button
                    }
                    if (!isLogin && password != confirmPassword) {
                        errorMessage = "Passwords don't match!"
                        return@Button
                    }
                    if (!isLogin && password.length < 6) {
                        errorMessage = "Password must be at least 6 characters!"
                        return@Button
                    }
                    if (isLogin) viewModel.login(email, password)
                    else viewModel.register(email, password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PurplePrimary, BluePrimary)
                            ),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isLogin) "Sign In" else "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2A2A4A)
                )
                Text("or", color = TextSecondary, fontSize = 13.sp)
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2A2A4A)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Guest Login Button ──
            OutlinedButton(
                onClick = {
                    errorMessage = ""
                    viewModel.continueAsGuest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF2A2A4A)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                enabled = !isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PersonOutline,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Continue as Guest",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Terms ──
            Text(
                text = "By continuing you agree to our Terms of Service\nand Privacy Policy",
                fontSize = 11.sp,
                color = Color(0xFF374151),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}