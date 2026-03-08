package com.trackit.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    onNavigateBack: () -> Unit,
    onAmountDetected: (Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Arahkan kamera ke struk belanja") }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Pindai Struk", fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Message
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = statusMessage,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Capture Button
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(72.dp),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                isProcessing = true
                                statusMessage = "Memproses..."

                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                val recognizer = TextRecognition.getClient(
                                                    TextRecognizerOptions.DEFAULT_OPTIONS
                                                )

                                                recognizer.process(image)
                                                    .addOnSuccessListener { visionText ->
                                                        val amount = extractLargestAmount(visionText.text)
                                                        imageProxy.close()

                                                        if (amount != null) {
                                                            onAmountDetected(amount)
                                                        } else {
                                                            isProcessing = false
                                                            statusMessage = "Tidak dapat mendeteksi nominal. Coba lagi."
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        imageProxy.close()
                                                        isProcessing = false
                                                        statusMessage = "Gagal memproses. Coba lagi."
                                                    }
                                            } else {
                                                imageProxy.close()
                                                isProcessing = false
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isProcessing = false
                                            statusMessage = "Gagal mengambil gambar. Coba lagi."
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f))
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Ambil Foto",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                // No camera permission
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Izin kamera diperlukan untuk memindai struk",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Berikan Izin")
                    }
                }
            }
        }
    }
}

/**
 * Extracts the largest numeric value from OCR text.
 * This is a heuristic approach — typically the total is the largest number on a receipt.
 */
private fun extractLargestAmount(text: String): Double? {
    val numberPattern = Regex("""[\d.,]+""")
    val matches = numberPattern.findAll(text)

    return matches
        .map { match ->
            match.value
                .replace(".", "")
                .replace(",", ".")
                .toDoubleOrNull() ?: 0.0
        }
        .filter { it > 0 }
        .maxOrNull()
}
