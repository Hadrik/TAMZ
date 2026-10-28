package com.example.tamz

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// UPC-A L a R patterny
private val Lp = intArrayOf(
    0x0D, // 0001101
    0x19, // 0011001
    0x13, // 0010011
    0x3D, // 0111101
    0x23, // 0100011
    0x31, // 0110001
    0x2F, // 0101111
    0x3B, // 0111011
    0x37, // 0110111
    0x0B  // 0001011
)

private val Rp = intArrayOf(
    0x72, // 1110010
    0x66, // 1100110
    0x6C, // 1101100
    0x42, // 1000010
    0x5C, // 1011100
    0x4E, // 1001110
    0x50, // 1010000
    0x44, // 1000100
    0x48, // 1001000
    0x74  // 1110100
)

@Composable
fun BarcodeScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanner = remember(context) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_UPC_A)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }
    var input by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf<String?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("Zadejte přesně 11 číslic") }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp)
        ) {
            barcode?.let { value ->
                val bits = upcABits(value)
                val moduleWidth = size.width / bits.size
                val barHeight = size.height * 0.78f

                bits.forEachIndexed { index, bit ->
                    if (bit) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(
                                x = index * moduleWidth,
                                y = 0f
                            ),
                            size = Size(
                                width = moduleWidth,
                                height = barHeight
                            )
                        )
                    }
                }

                drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.BLACK
                        textAlign = Paint.Align.CENTER
                        textSize = 16.dp.toPx()
                    }
                    val textBaseline = size.height * 0.96f

                    value.forEachIndexed { index, digit ->
                        val moduleStart = if (index < 6) {
                            3 + index * 7
                        } else {
                            50 + (index - 6) * 7
                        }
                        val center = (moduleStart + 3.5f) * moduleWidth
                        canvas.nativeCanvas.drawText(
                            digit.toString(),
                            center,
                            textBaseline,
                            paint
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = {
                input = it
                hasError = false
            },
            label = { Text("Číslo") },
            singleLine = true,
            isError = hasError,
            supportingText = if (hasError) {
                { Text(errorMessage) }
            } else {
                null
            }
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically),
                onClick = {
                    if (input.matches(Regex("\\d{11}"))) {
                        barcode = input + calculateCheckDigit(input)
                        hasError = false
                    } else {
                        barcode = null
                        errorMessage = "Zadejte přesně 11 číslic"
                        hasError = true
                    }
                }
            ) {
                Text(text = "Vygenerovat")
            }

            Button(
                modifier = Modifier.padding(start = 8.dp).align(Alignment.CenterVertically),
                onClick = {
                    scanner.startScan()
                        .addOnSuccessListener { result ->
                            val scannedValue = result.rawValue
                            if (scannedValue != null && isValidUpcA(scannedValue)) {
                                input = scannedValue.dropLast(1)
                                barcode = scannedValue
                                hasError = false
                            } else {
                                barcode = null
                                errorMessage = "Naskenovaný kód není platný UPC-A"
                                hasError = true
                            }
                        }
                        .addOnFailureListener {
                            errorMessage = "Skenování se nepodařilo"
                            hasError = true
                        }
                }
            ) {
                Text(text = "Naskenovat")
            }
        }
    }
}

private fun isValidUpcA(value: String): Boolean =
    value.matches(Regex("\\d{12}")) &&
        value.last() == calculateCheckDigit(value.dropLast(1))

private fun upcABits(value: String): List<Boolean> = buildList {
    // Start
    addAll(listOf(true, false, true))

    // L
    value.take(6).forEach { digit ->
        addAll(patternBits(Lp[digit - '0']))
    }

    // Middle
    addAll(listOf(false, true, false, true, false))

    // R
    value.drop(6).forEach { digit ->
        addAll(patternBits(Rp[digit - '0']))
    }

    // End
    addAll(listOf(true, false, true))
}

private fun calculateCheckDigit(value: String): Char {
    val weightedSum = value.mapIndexed { index, digit ->
        (digit - '0') * if (index % 2 == 0) 3 else 1
    }.sum()
    val mod = weightedSum % 10
    return ('0'.code + if (mod == 0) 0 else (10 - mod)).toChar()
}

private fun patternBits(pattern: Int): List<Boolean> =
    (6 downTo 0).map { bit -> pattern and (1 shl bit) != 0 }
