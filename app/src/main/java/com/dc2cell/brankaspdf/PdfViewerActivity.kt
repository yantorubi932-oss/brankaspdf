package com.dc2cell.brankaspdf

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.launch
import java.io.File

class PdfViewerActivity : AppCompatActivity() {
    private lateinit var previewFile: File
    private lateinit var downloadedFile: File
    private var mimeType = "application/octet-stream"
    private lateinit var shareButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val name = intent.getStringExtra("name") ?: "download"
        val path = intent.getStringExtra("storagePath") ?: return finish()
        mimeType = intent.getStringExtra("mimeType") ?: "application/octet-stream"
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        previewFile = File(cacheDir, "preview/$safeName"); downloadedFile = File(getExternalFilesDir("shared"), safeName)
        previewFile.parentFile?.mkdirs(); downloadedFile.parentFile?.mkdirs()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val pdfView = PDFView(this, null); root.addView(pdfView, LinearLayout.LayoutParams(-1, 0, 1f))
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        val download = Button(this).apply { text = "Download" }
        shareButton = Button(this).apply { text = "Share"; isEnabled = downloadedFile.exists() }
        actions.addView(download, LinearLayout.LayoutParams(0, -2, 1f)); actions.addView(shareButton, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(actions); setContentView(root)
        lifecycleScope.launch {
            runCatching { Supabase.client.storage.from("documents").download(path) }.onSuccess { bytes -> previewFile.writeBytes(bytes); if (mimeType == "application/pdf" || name.endsWith(".pdf", true)) pdfView.fromFile(previewFile).load() else Toast.makeText(this@PdfViewerActivity, "Preview tersedia untuk PDF. Download untuk file ini.", Toast.LENGTH_LONG).show() }.onFailure { Toast.makeText(this@PdfViewerActivity, "Preview gagal: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
        download.setOnClickListener { lifecycleScope.launch { runCatching { Supabase.client.storage.from("documents").download(path) }.onSuccess { downloadedFile.writeBytes(it); shareButton.isEnabled = true; Toast.makeText(this@PdfViewerActivity, "File sudah di-download", Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(this@PdfViewerActivity, "Download gagal: ${it.localizedMessage}", Toast.LENGTH_LONG).show() } } }
        shareButton.setOnClickListener { shareDownloaded() }
    }

    private fun shareDownloaded() {
        if (!downloadedFile.exists()) return
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", downloadedFile)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, "Bagikan file yang sudah di-download"))
    }
}
