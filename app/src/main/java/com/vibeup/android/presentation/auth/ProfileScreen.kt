package com.vibeup.android.presentation.auth

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vibeup.android.Screen
import androidx.compose.material.icons.filled.Download
import com.vibeup.android.presentation.stats.StatsWidget
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ChevronRight
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val profileState by profileViewModel.profileState.collectAsState()
    val displayName by profileViewModel.displayName.collectAsState()
    val photoUrl by profileViewModel.photoUrl.collectAsState()

    var nickname by remember { mutableStateOf(displayName) }
    var isEditingName by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showSnackbar by remember { mutableStateOf(false) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { profileViewModel.uploadProfilePicture(it) }
    }

    // Handle state changes
    LaunchedEffect(profileState) {
        when (profileState) {
            is ProfileState.Success -> {
                snackbarMessage = (profileState as ProfileState.Success).message
                showSnackbar = true
                profileViewModel.resetState()
            }
            is ProfileState.Error -> {
                snackbarMessage = (profileState as ProfileState.Error).message
                showSnackbar = true
                profileViewModel.resetState()
            }
            else -> {}
        }
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to logout?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.logout()
                        showLogoutDialog = false
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBackIosNew,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Picture
            Box(contentAlignment = Alignment.BottomEnd) {
                if (photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                // Camera button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Nickname Field
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Display Name",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isEditingName) {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            singleLine = true,
                            trailingIcon = {
                                TextButton(onClick = {
                                    profileViewModel.updateProfile(nickname)
                                    isEditingName = false
                                }) {
                                    Text("Save", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName.ifEmpty { "Set your nickname" },
                                fontSize = 16.sp,
                                color = if (displayName.isEmpty())
                                    MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                            )
                            IconButton(onClick = {
                                nickname = displayName
                                isEditingName = true
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Email (Read only)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Email",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = profileViewModel.email,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reset Password
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { profileViewModel.sendPasswordResetEmail() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Reset Password",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Send reset link to your email",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Downloads
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Downloads.route) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Downloads",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Manage your offline music",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            StatsWidget(
                onOpenStats = { navController.navigate(Screen.Stats.route) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Appearance / Settings
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Settings.route) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Appearance",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Obsidian, Aurora, Crimson & more",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator
            if (profileState is ProfileState.Loading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            // Snackbar message
            if (showSnackbar) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (profileState is ProfileState.Error)
                            Color.Red.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                ) {
                    Text(
                        text = snackbarMessage,
                        modifier = Modifier.padding(12.dp),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                LaunchedEffect(showSnackbar) {
                    kotlinx.coroutines.delay(3000)
                    showSnackbar = false
                }
            }
        }
    }
}