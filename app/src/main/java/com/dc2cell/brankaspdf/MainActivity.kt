package com.dc2cell.brankaspdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dc2cell.brankaspdf.databinding.ActivityMainBinding
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CloudFile(
    val id: String = "",
    @SerialName("owner_id") val ownerId: String = "",
    val name: String = "",
    @SerialName("storage_path") val storagePath: String = "",
    @SerialName("mime_type") val mimeType: String = "application/octet-stream",
    val size: Long = 0,
    @SerialName("created_at") val createdAt: String? = null
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: PdfAdapter
    private var unlocked = false
    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(::uploadFile) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        adapter = PdfAdapter { openFile(it) }
        binding.recycler.layoutManager = LinearLayoutManager(this); binding.recycler.adapter = adapter
        binding.btnUpload.setOnClickListener { pickFile.launch(arrayOf("application/pdf", "image/*", "text/*", "application/zip", "application/msword")) }
        binding.btnScan.setOnClickListener { startActivity(Intent(this, ScanActivity::class.java)) }
        binding.btnLogout.setOnClickListener { lifecycleScope.launch { Supabase.client.auth.signOut(); startActivity(Intent(this@MainActivity, LoginActivity::class.java)); finish() } }
        authenticateApp()
    }

    private fun authenticateApp() {
        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(allowed) != BiometricManager.BIOMETRIC_SUCCESS) { unlocked = true; loadFiles(); return }
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { unlocked = true; loadFiles() }
            override fun onAuthenticationError(code: Int, message: CharSequence) { Toast.makeText(this@MainActivity, "Aplikasi terkunci: $message", Toast.LENGTH_LONG).show() }
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Buka Brankas PDF").setSubtitle("Gunakan fingerprint atau kunci layar").setAllowedAuthenticators(allowed).build())
    }

    private fun loadFiles() = lifecycleScope.launch {
        runCatching {
            Supabase.client.from("files").select { order("created_at", Order.DESCENDING) }.decodeList<CloudFile>()
        }.onSuccess { list -> adapter.submitList(list); binding.emptyView.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE }
            .onFailure { Toast.makeText(this@MainActivity, "Gagal memuat file: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
    }

    private fun uploadFile(uri: Uri) {
        if (!unlocked) return
        val user = Supabase.client.auth.currentUserOrNull() ?: return
        val name = queryName(uri); val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        val size = contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { if (it.moveToFirst()) it.getLong(0) else 0L } ?: 0L
        if (size > 50L * 1024 * 1024) { Toast.makeText(this, "Ukuran maksimal 50 MB", Toast.LENGTH_LONG).show(); return }
        val path = "${user.id}/${UUID.randomUUID()}_${name.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
        lifecycleScope.launch {
            runCatching {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("File tidak dapat dibaca")
                Supabase.client.storage.from("documents").upload(path, bytes, upsert = false)
                Supabase.client.from("files").insert(mapOf("owner_id" to user.id, "name" to name, "storage_path" to path, "mime_type" to mime, "size" to bytes.size))
            }.onSuccess { Toast.makeText(this@MainActivity, "File tersimpan", Toast.LENGTH_SHORT).show(); loadFiles() }
                .onFailure { Toast.makeText(this@MainActivity, "Upload gagal: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }

    private fun openFile(file: CloudFile) = startActivity(Intent(this, PdfViewerActivity::class.java).apply { putExtra("fileId", file.id); putExtra("storagePath", file.storagePath); putExtra("name", file.name); putExtra("mimeType", file.mimeType) })
    private fun queryName(uri: Uri): String = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "file_${System.currentTimeMillis()}"
}
