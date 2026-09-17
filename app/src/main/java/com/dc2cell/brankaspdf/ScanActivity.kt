package com.dc2cell.brankaspdf

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dc2cell.brankaspdf.databinding.ActivityScanBinding
import kotlinx.coroutines.launch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

class ScanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScanBinding
    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); binding = ActivityScanBinding.inflate(layoutInflater); setContentView(binding.root)
        startCamera(); binding.btnCapture.setOnClickListener { takePhoto() }
    }
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ val provider = future.get(); val preview = Preview.Builder().build().also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }; imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build(); provider.unbindAll(); provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture) }, ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        val file = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        imageCapture?.takePicture(ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback { override fun onImageSaved(output: ImageCapture.OutputFileResults) { saveAsPdf(file) }; override fun onError(exc: ImageCaptureException) { Toast.makeText(this@ScanActivity, "Gagal foto: ${exc.localizedMessage}", Toast.LENGTH_LONG).show() } })
    }
    private fun saveAsPdf(jpg: File) = lifecycleScope.launch {
        runCatching {
            val bitmap = android.graphics.BitmapFactory.decodeFile(jpg.absolutePath) ?: error("Foto tidak dapat dibaca")
            val doc = PDDocument(); val page = PDPage(); doc.addPage(page); val jpeg = ByteArrayOutputStream(); bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, jpeg); bitmap.recycle(); val compressed = android.graphics.BitmapFactory.decodeByteArray(jpeg.toByteArray(), 0, jpeg.size()) ?: error("Kompresi gagal"); val image = JPEGFactory.createFromImage(doc, compressed); val scale = minOf(page.mediaBox.width / image.width, page.mediaBox.height / image.height); val width = image.width * scale; val height = image.height * scale; PDPageContentStream(doc, page).use { it.drawImage(image, (page.mediaBox.width - width) / 2, (page.mediaBox.height - height) / 2, width, height) }; val output = ByteArrayOutputStream(); doc.save(output); doc.close(); compressed.recycle()
            val user = Supabase.client.auth.currentUserOrNull() ?: error("Belum login"); val path = "${user.id}/${UUID.randomUUID()}.pdf"; val bytes = output.toByteArray(); Supabase.client.storage.from("documents").upload(path, bytes, upsert = false); Supabase.client.from("files").insert(mapOf("owner_id" to user.id, "name" to "Scan_${System.currentTimeMillis()}.pdf", "storage_path" to path, "mime_type" to "application/pdf", "size" to bytes.size))
        }.onSuccess { Toast.makeText(this@ScanActivity, "Scan tersimpan", Toast.LENGTH_SHORT).show(); finish() }.onFailure { Toast.makeText(this@ScanActivity, "Scan gagal: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
    }
}
