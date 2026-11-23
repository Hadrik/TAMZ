package com.example.tamz

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tamz.ui.theme.TAMZTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TAMZTheme {
                Detection()
            }
        }
    }
}

@Composable
fun Detection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val originalBitmap = remember(context) {
        BitmapFactory.decodeResource(context.applicationContext.resources, R.drawable.big_e)
    }

    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var smileList by remember { mutableStateOf<List<String>>(emptyList()) }

    val detector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
        FaceDetection.getClient(options)
    }

    val currentBitmap = displayedBitmap ?: originalBitmap

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Detected image",
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )

                    if (smileList.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .height(100.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                smileList.forEach { smileInfo ->
                                    Text(
                                        text = smileInfo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Buttons(
                onDetect = {
                    smileList = emptyList()
                    val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val inputImage = InputImage.fromBitmap(mutableBitmap, 0)
                    detector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            val canvas = Canvas(mutableBitmap)
                            val strokeWidth = (mutableBitmap.width / 100f).coerceAtLeast(6f)
                            val paint = Paint().apply {
                                color = Color.RED
                                style = Paint.Style.STROKE
                                this.strokeWidth = strokeWidth
                            }
                            for (face in faces) {
                                canvas.drawRect(face.boundingBox, paint)
                            }
                            displayedBitmap = mutableBitmap
                        }
                },
                onAnonymize = {
                    smileList = emptyList()
                    val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val inputImage = InputImage.fromBitmap(mutableBitmap, 0)
                    detector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            val canvas = Canvas(mutableBitmap)
                            val paint = Paint().apply {
                                color = Color.BLACK
                                style = Paint.Style.FILL
                            }
                            for (face in faces) {
                                canvas.drawRect(face.boundingBox, paint)
                            }
                            displayedBitmap = mutableBitmap
                        }
                },
                onSmile = {
                    val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val inputImage = InputImage.fromBitmap(mutableBitmap, 0)
                    detector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            val canvas = Canvas(mutableBitmap)
                            val boxPaint = Paint().apply {
                                color = Color.GREEN
                                style = Paint.Style.STROKE
                                strokeWidth = (mutableBitmap.width / 120f).coerceAtLeast(4f)
                            }
                            val badgeRadius = (mutableBitmap.width / 45f).coerceAtLeast(16f)
                            val bgPaint = Paint().apply {
                                color = Color.RED
                                style = Paint.Style.FILL
                            }
                            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = Color.WHITE
                                textSize = badgeRadius * 1.2f
                                typeface = Typeface.DEFAULT_BOLD
                                textAlign = Paint.Align.CENTER
                            }

                            val newSmileList = mutableListOf<String>()

                            faces.forEachIndexed { index, face ->
                                val faceNumber = index + 1
                                val bounds = face.boundingBox

                                canvas.drawRect(bounds, boxPaint)

                                val cx = bounds.left.toFloat() + badgeRadius
                                val cy = bounds.top.toFloat() + badgeRadius
                                canvas.drawCircle(cx, cy, badgeRadius, bgPaint)

                                val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2)
                                canvas.drawText("$faceNumber", cx, textY, textPaint)

                                val smileProb = face.smilingProbability ?: 0f
                                val formattedSmile = String.format(Locale.US, "%.2f", smileProb)
                                newSmileList.add("Face #$faceNumber - smile $formattedSmile")
                            }

                            displayedBitmap = mutableBitmap
                            smileList = newSmileList
                        }
                },
                onSave = {
                    val bitmapToSave = displayedBitmap ?: originalBitmap
                    saveBitmapToGallery(context, bitmapToSave)
                },
                onReset = {
                    displayedBitmap = originalBitmap
                    smileList = emptyList()
                }
            )
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "TAMZ_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TAMZ")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (uri != null) {
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving image", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "Error creating media file", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun Buttons(
    onDetect: () -> Unit,
    onAnonymize: () -> Unit,
    onSmile: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDetect,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text("Detekce")
                }
                Button(
                    onClick = onAnonymize,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text("Anonymizace")
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onSmile,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text("Úsměv")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                ) {
                    Text("Uložit")
                }
            }
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                ) {
                    Text("Reset")
            }
        }
    }
}