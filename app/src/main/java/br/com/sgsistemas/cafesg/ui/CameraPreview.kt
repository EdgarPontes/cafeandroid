package br.com.sgsistemas.cafesg.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.camera.core.*
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class CameraController(val context: Context) {
    val controller = LifecycleCameraController(context)
    
    init {
        controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    }

    fun takePhoto(onPhotoCaptured: (String) -> Unit) {
        controller.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    
                    val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                    
                    val outputStream = ByteArrayOutputStream()
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                    val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                    
                    onPhotoCaptured(base64)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraPreview", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        // Front camera is often mirrored, we want the "real" view usually or keep it mirrored as it was in preview
        // For selfie style, we might want to mirror it back.
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifecycleOwner)
            }
        },
        modifier = modifier
    )
}
