package com.dc2cell.brankaspdf

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dc2cell.brankaspdf.databinding.ActivityLoginBinding
import io.github.jan.supabase.auth.providers.Email
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.signInButton.setOnClickListener { authenticate(false) }
        binding.signUpButton.setOnClickListener { authenticate(true) }
    }

    private fun authenticate(signUp: Boolean) {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.length < 6) {
            Toast.makeText(this, "Masukkan email valid dan password minimal 6 karakter", Toast.LENGTH_LONG).show()
            return
        }
        binding.progress.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            runCatching {
                if (signUp) Supabase.client.auth.signUpWith(Email) { this.email = email; this.password = password }
                else Supabase.client.auth.signInWith(Email) { this.email = email; this.password = password }
            }.onSuccess {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java)); finish()
            }.onFailure { Toast.makeText(this@LoginActivity, it.localizedMessage ?: "Auth gagal", Toast.LENGTH_LONG).show() }
            binding.progress.visibility = android.view.View.GONE
        }
    }
}
