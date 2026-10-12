package hapsari.ayudyadiah.sdnpojok2app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import hapsari.ayudyadiah.sdnpojok2app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()

        b.btnLogin.setOnClickListener { login() }
        b.btnRegister.setOnClickListener { register() }
    }

    override fun onStart() {
        super.onStart()
        // Kalau sebelumnya sudah login, langsung masuk ke MainActivity
        if (auth.currentUser != null) {
            goToMain()
        }
    }

    private fun login() {
        val email = b.edEmail.text.toString().trim()
        val password = b.edPassword.text.toString().trim()

        if (!validate(email, password)) return

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    goToMain()
                } else {
                    b.tvError.text = describeAuthError(task.exception, isRegister = false)
                }
            }
    }

    private fun register() {
        val email = b.edEmail.text.toString().trim()
        val password = b.edPassword.text.toString().trim()

        if (!validate(email, password)) return

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    goToMain()
                } else {
                    b.tvError.text = describeAuthError(task.exception, isRegister = true)
                }
            }
    }

    private fun validate(email: String, password: String): Boolean {
        b.tvError.text = ""
        if (email.isEmpty() || password.isEmpty()) {
            b.tvError.text = "Email dan password wajib diisi"
            return false
        }
        if (password.length < 6) {
            b.tvError.text = "Password minimal 6 karakter"
            return false
        }
        return true
    }

    private fun describeAuthError(e: Exception?, isRegister: Boolean): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password terlalu lemah, minimal 6 karakter"
        is FirebaseAuthInvalidCredentialsException -> "Format email tidak valid"
        is FirebaseAuthUserCollisionException -> "Email sudah terdaftar, silakan masuk"
        is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan atau password salah"
        is FirebaseException -> e.message ?: "Terjadi kesalahan, periksa koneksi internet"
        else -> if (isRegister) "Gagal mendaftar, coba lagi" else "Gagal masuk, periksa email dan password"
    }

    private fun setLoading(loading: Boolean) {
        b.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnLogin.isEnabled = !loading
        b.btnRegister.isEnabled = !loading
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}