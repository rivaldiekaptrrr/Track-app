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
import com.google.mlkit.vision.text.Text
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
                                                        val result = extractTotalAmount(visionText)
                                                        imageProxy.close()

                                                        when (result) {
                                                            is OcrResult.Success -> onAmountDetected(result.amount)
                                                            is OcrResult.Error -> {
                                                                isProcessing = false
                                                                statusMessage = result.reason
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        imageProxy.close()
                                                        isProcessing = false
                                                        statusMessage = "Error OCR: ${it.message}"
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

private const val MIN_AMOUNT = 1000.0
private const val MAX_AMOUNT = 500_000_000.0

private sealed class OcrResult {
    data class Success(val amount: Double) : OcrResult()
    data class Error(val reason: String) : OcrResult()
}

/**
 * Extracts the total payment amount from OCR text.
 * Uses multi-pass approach with keyword search and position awareness.
 */
private fun extractTotalAmount(visionText: Text): OcrResult {
    val keywordPriority = mapOf(
        "GRAND TOTAL" to 300,
        "GRANDTOTAL" to 290,
        "TOTAL" to 200,
        "JUMLAH" to 180,
        "PAYMENT DUE" to 170,
        "AMOUNT DUE" to 160,
        "SUBTOTAL" to 150,
        "TAGIHAN" to 140,
        "BAYAR" to 130,
        "NETTO" to 120,
        "SALDO" to 110
    )

    val candidateAmounts = mutableListOf<Pair<Double, Int>>()
    var hasKeyword = false
    var hasNumber = false

    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val lineText = line.text
            val upperLine = lineText.uppercase()

            val matchedKeyword = keywordPriority.entries
                .filter { (keyword, _) -> upperLine.contains(keyword) }
                .maxByOrNull { it.value }

            if (matchedKeyword != null) {
                hasKeyword = true
                val amountAfterKeyword = extractAmountAfterKeyword(lineText, matchedKeyword.key)
                if (amountAfterKeyword != null) {
                    hasNumber = true
                    if (amountAfterKeyword in MIN_AMOUNT..MAX_AMOUNT) {
                        candidateAmounts.add(amountAfterKeyword to matchedKeyword.value)
                    }
                }
            } else if (keywordPriority.keys.any { upperLine.contains(it) }) {
                hasKeyword = true
            }
        }
    }

    if (candidateAmounts.isNotEmpty()) {
        return OcrResult.Success(candidateAmounts.maxByOrNull { it.second }!!.first)
    }

    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val lineText = line.text
            val upperLine = lineText.uppercase()

            if (upperLine.contains("TOTAL") || upperLine.contains("JUMLAH")) {
                hasKeyword = true
                val amounts = extractAmountsFromLineRobust(lineText)
                for (amount in amounts) {
                    hasNumber = true
                    if (amount in MIN_AMOUNT..MAX_AMOUNT) {
                        candidateAmounts.add(amount to 50)
                    }
                }
            }
        }
    }

    if (candidateAmounts.isNotEmpty()) {
        return OcrResult.Success(candidateAmounts.maxByOrNull { it.second }!!.first)
    }

    val allAmounts = mutableListOf<Double>()
    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val amounts = extractAmountsFromLineRobust(line.text)
            if (amounts.isNotEmpty()) {
                hasNumber = true
            }
            allAmounts.addAll(amounts)
        }
    }

    val validAmounts = allAmounts.filter { it in MIN_AMOUNT..MAX_AMOUNT }
    if (validAmounts.isNotEmpty()) {
        return OcrResult.Success(validAmounts.maxOrNull()!!)
    }

    return when {
        !hasKeyword -> OcrResult.Error("Tidak ada keyword TOTAL/JUMLAH")
        !hasNumber -> OcrResult.Error("Tidak ada angka di struk")
        else -> OcrResult.Error("Format tidak valid (gunakan format: 150.000)")
    }
}

private fun extractAmountAfterKeyword(line: String, keyword: String): Double? {
    val upperLine = line.uppercase()
    val upperKeyword = keyword.uppercase()
    
    val keywordIndex = upperLine.indexOf(upperKeyword)
    if (keywordIndex == -1) return null

    val afterKeyword = line.substring(keywordIndex + keyword.length)
    
    val numberPattern = Regex("""[\d][\d,\.\s]*[\d]""")
    val match = numberPattern.find(afterKeyword)
    
    val value = match?.value ?: return null
    val digitsOnly = value.filter { it.isDigit() }
    
    if (digitsOnly.length > 12) return null
    if (!isValidIndonesianCurrency(value)) return null
    
    val amount = cleanNumber(value).toDoubleOrNull() ?: return null
    
    return if (amount in MIN_AMOUNT..MAX_AMOUNT) amount else null
}

private fun extractAmountsFromLineRobust(line: String): List<Double> {
    val amounts = mutableListOf<Double>()

    val numberWithSeparator = Regex("""[\d]+[.,\s][\d]+""").findAll(line)
    for (match in numberWithSeparator) {
        val originalValue = match.value
        if (!isValidIndonesianCurrency(originalValue)) continue
        
        val cleaned = cleanNumber(originalValue)
        cleaned.toDoubleOrNull()?.let { 
            if (it in MIN_AMOUNT..MAX_AMOUNT) amounts.add(it) 
        }
    }

    return amounts.distinct()
}

private fun isValidIndonesianCurrency(value: String): Boolean {
    val trimmed = value.trim()
    
    if (!trimmed.contains('.') && !trimmed.contains(',')) {
        return false
    }
    
    val parts = when {
        trimmed.contains('.') && trimmed.contains(',') -> {
            if (trimmed.lastIndexOf(',') > trimmed.lastIndexOf('.')) {
                trimmed.replace(".", "").split(",")
            } else {
                trimmed.replace(",", "").split(".")
            }
        }
        trimmed.contains('.') -> trimmed.split(".")
        trimmed.contains(',') -> trimmed.split(",")
        else -> return false
    }
    
    if (parts.size != 2) return false
    
    val decimalPart = parts[1]
    if (decimalPart.length != 3) return false
    
    val decimalValue = decimalPart.toIntOrNull() ?: return false
    
    return decimalValue == 0 || decimalValue == 500
}

private fun cleanNumber(value: String): String {
    var cleaned = value.trim()

    if (cleaned.endsWith("-") || cleaned.endsWith(".")) {
        cleaned = cleaned.dropLast(1)
    }

    val hasComma = cleaned.contains(',')
    val hasDot = cleaned.contains('.')

    when {
        hasComma && hasDot -> {
            val lastCommaIndex = cleaned.lastIndexOf(',')
            val lastDotIndex = cleaned.lastIndexOf('.')
            if (lastCommaIndex > lastDotIndex) {
                cleaned = cleaned.replace(".", "").replace(",", ".")
            } else {
                cleaned = cleaned.replace(",", "")
            }
        }
        hasDot -> {
            val parts = cleaned.split(".")
            if (parts.size == 2 && parts[1].length == 3) {
                cleaned = cleaned.replace(".", "")
            } else {
                cleaned = cleaned.replace(",", "")
            }
        }
        hasComma -> {
            cleaned = cleaned.replace(",", ".")
        }
    }

    return cleaned
}

private fun isValidAmount(amount: Double): Boolean {
    return amount in 1000.0..500_000_000.0
}
