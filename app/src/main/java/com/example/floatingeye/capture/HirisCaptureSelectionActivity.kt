package com.example.floatingeye.capture

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File

/** Lets the user draw a rectangle over the captured screen, then saves only that crop. */
class HirisCaptureSelectionActivity : ComponentActivity() {
    private var overlayWasRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayWasRunning = com.example.floatingeye.overlay.FloatingEyeOverlayService.isServiceRunning.value
        if (overlayWasRunning) com.example.floatingeye.overlay.FloatingEyeOverlayService.stop(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val imageFile = intent.getStringExtra(HirisCaptureService.EXTRA_IMAGE_PATH)?.let(::File)
        val bitmap = imageFile?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
        if (bitmap == null) {
            Toast.makeText(this, "Screenshot not found. Take a new screenshot.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContent {
            MaterialTheme {
                var dragStart by remember { mutableStateOf<Offset?>(null) }
                var dragEnd by remember { mutableStateOf<Offset?>(null) }
                var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
                val image = remember(bitmap) { bitmap.asImageBitmap() }
                val scale = if (canvasSize.width > 0f && canvasSize.height > 0f) {
                    minOf(canvasSize.width / bitmap.width, canvasSize.height / bitmap.height)
                } else 1f
                val left = (canvasSize.width - bitmap.width * scale) / 2f
                val top = (canvasSize.height - bitmap.height * scale) / 2f
                val selection = if (dragStart != null && dragEnd != null) {
                    Rect(dragStart!!, dragEnd!!).normalized()
                } else null

                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    Box(Modifier.fillMaxSize()) {
                        Canvas(
                            Modifier.fillMaxSize().onSizeChanged {
                                canvasSize = androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat())
                            }.pointerInput(bitmap, scale, left, top) {
                                detectDragGestures(
                                    onDragStart = { point -> dragStart = point; dragEnd = point },
                                    onDrag = { change, _ -> dragEnd = change.position }
                                )
                            }
                        ) {
                            drawImage(
                                image,
                                dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize((bitmap.width * scale).toInt(), (bitmap.height * scale).toInt())
                            )
                            selection?.let { rect ->
                                drawRect(Color(0x6600CFFF), topLeft = rect.topLeft, size = rect.size)
                                drawRect(Color(0xFF00CFFF), topLeft = rect.topLeft, size = rect.size, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                            }
                        }
                        Column(
                            Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color(0xCC101622)).padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Draw the area to save", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("Drag your finger across the screenshot, then tap Save area.", color = Color.LightGray)
                        }
                        Row(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0xDD101622)).padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = { imageFile.delete(); bitmap.recycle(); finish() }, modifier = Modifier.weight(1f)) {
                                Text("Cancel")
                            }
                            Button(
                                enabled = selection != null && selection.width > 8f && selection.height > 8f,
                                onClick = {
                                    val rect = selection ?: return@Button
                                    val x1 = ((rect.left - left) / scale).toInt().coerceIn(0, bitmap.width - 1)
                                    val y1 = ((rect.top - top) / scale).toInt().coerceIn(0, bitmap.height - 1)
                                    val x2 = ((rect.right - left) / scale).toInt().coerceIn(x1 + 1, bitmap.width)
                                    val y2 = ((rect.bottom - top) / scale).toInt().coerceIn(y1 + 1, bitmap.height)
                                    val crop = Bitmap.createBitmap(bitmap, x1, y1, x2 - x1, y2 - y1)
                                    val values = ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, "Hiris_crop_${System.currentTimeMillis()}.png")
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                                        if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hiris Capture")
                                    }
                                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                    val saved = uri != null && contentResolver.openOutputStream(uri)?.use {
                                        crop.compress(Bitmap.CompressFormat.PNG, 100, it)
                                    } == true
                                    crop.recycle()
                                    imageFile.delete()
                                    bitmap.recycle()
                                    Toast.makeText(this@HirisCaptureSelectionActivity, if (saved) "Selected screenshot saved" else "Could not save screenshot", Toast.LENGTH_LONG).show()
                                    finish()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Save area") }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (overlayWasRunning && !com.example.floatingeye.overlay.FloatingEyeOverlayService.isServiceRunning.value) {
            com.example.floatingeye.overlay.FloatingEyeOverlayService.start(this)
        }
        super.onDestroy()
    }
}

private fun Rect.normalized() = Rect(
    left = minOf(left, right), top = minOf(top, bottom),
    right = maxOf(left, right), bottom = maxOf(top, bottom)
)
