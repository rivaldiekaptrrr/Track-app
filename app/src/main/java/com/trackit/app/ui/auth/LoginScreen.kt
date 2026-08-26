package com.trackit.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.platform.LocalContext
import com.trackit.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSkip: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    val salmonCoral = Color(0xFFFD827E)
    val textDark = Color(0xFF2C2C2C)

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoginSuccess()
    }

    val context = LocalContext.current
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                viewModel.loginWithGoogle(idToken)
            } ?: viewModel.setError("Gagal mendapatkan token Google.")
        } catch (e: ApiException) {
            if (e.statusCode != 12501) {
                viewModel.setError("Google Sign-In error (code: ${e.statusCode}). Pastikan SHA-1 sudah terdaftar di Firebase Console.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(800)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Wavy Topographic Header Shape (Tinggi sekitar 38%)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        // Draw coral wavy background shape
                        val backgroundPath = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(width, 0f)
                            lineTo(width, height * 0.72f)
                            cubicTo(
                                width * 0.7f, height * 0.9f,
                                width * 0.35f, height * 0.6f,
                                0f, height * 0.82f
                            )
                            close()
                        }
                        drawPath(path = backgroundPath, color = salmonCoral)

                        // Clip topographic lines inside the background shape
                        clipPath(backgroundPath) {
                            val lineCount = 10
                            for (i in -lineCount..lineCount) {
                                val offset = i * 22.dp.toPx()
                                val path = Path().apply {
                                    moveTo(0f, height * 0.82f + offset)
                                    cubicTo(
                                        width * 0.35f, height * 0.6f + offset * 0.8f,
                                        width * 0.7f, height * 0.9f + offset * 1.2f,
                                        width, height * 0.72f + offset
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = Color.White.copy(alpha = 0.08f),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                }

                // 2. Form Content Section (Padding horizontal 32dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header "Sign in" / "Buat Akun"
                    AnimatedContent(targetState = isRegisterMode, label = "HeaderTitle") { register ->
                        Column {
                            Text(
                                text = if (register) "Sign up" else "Sign in",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Line indicator di bawah Title
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .height(4.dp)
                                    .background(salmonCoral, shape = RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Form Fields minimalis (Underline style)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Email Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Email",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textDark.copy(alpha = 0.7f)
                            )
                            TextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { Text("demo@email.com", color = Color.LightGray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = salmonCoral)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = salmonCoral,
                                    unfocusedIndicatorColor = Color.LightGray,
                                    focusedLabelColor = salmonCoral,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                singleLine = true
                            )
                        }

                        // Password Field
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Password",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textDark.copy(alpha = 0.7f)
                            )
                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("enter your password", color = Color.LightGray) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = salmonCoral)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = salmonCoral,
                                    unfocusedIndicatorColor = Color.LightGray,
                                    focusedLabelColor = salmonCoral,
                                    unfocusedLabelColor = Color.Gray
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Remember Me & Forgot Password Row
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = salmonCoral,
                                    uncheckedColor = Color.LightGray,
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                text = "Remember Me",
                                fontSize = 13.sp,
                                color = textDark.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(
                            onClick = {
                                // Simple toast for informational purposes
                                android.widget.Toast.makeText(context, "Silakan hubungi administrator jika Anda lupa password.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "Forgot Password?",
                                fontSize = 13.sp,
                                color = salmonCoral,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Error Message
                    AnimatedVisibility(visible = uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    // Primary Login Button (Coral colored)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (isRegisterMode) viewModel.registerWithEmail(email, password)
                            else viewModel.loginWithEmail(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = email.isNotBlank() && password.isNotBlank() && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = salmonCoral,
                            disabledContainerColor = salmonCoral.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            AnimatedContent(targetState = isRegisterMode, label = "ButtonText") { register ->
                                Text(
                                    text = if (register) "Sign Up" else "Login",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Toggle register/login
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isRegisterMode) "Already have an Account? " else "Don't have an Account? ",
                            fontSize = 14.sp,
                            color = textDark.copy(alpha = 0.8f)
                        )
                        Text(
                            text = if (isRegisterMode) "Sign in" else "Sign up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = salmonCoral,
                            modifier = Modifier.clickable {
                                isRegisterMode = !isRegisterMode
                                viewModel.clearError()
                            }
                        )
                    }

                    // Divider "atau"
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                        Text(
                            " atau ",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    // Google Login Button (Clean White + Official Google Logo)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { launcher.launch(googleSignInClient.signInIntent) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.7f), RoundedCornerShape(14.dp)),
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = textDark
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            tint = Color.Unspecified, // Tetap gunakan warna asli di XML
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Lanjutkan dengan Google",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = textDark
                        )
                    }

                    // Skip / Offline Mode Button
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.skipLogin()
                            onSkip()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.7f))
                    ) {
                        Text(
                            "Lewati, gunakan mode Offline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }
        }
    }
}
