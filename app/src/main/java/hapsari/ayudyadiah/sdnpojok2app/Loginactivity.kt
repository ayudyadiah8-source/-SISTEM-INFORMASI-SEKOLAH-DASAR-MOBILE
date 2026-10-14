package hapsari.ayudyadiah.sdnpojok2app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import hapsari.ayudyadiah.sdnpojok2app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()
        b.btnRegister.visibility = View.GONE
        b.btnLogin.setOnClickListener { loginAdmin() }
    }

    private fun loginAdmin() {
        val email = b.edEmail.text.toString().trim()
        val password = b.edPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            b.tvError.text = "Email dan password wajib diisi"
            b.tvError.visibility = View.VISIBLE
            return
        }

        setLoading(true)
        b.tvError.visibility = View.GONE

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // AMBIL TOKEN FIREBASE (JWT) BUKAN UID
                    auth.currentUser?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        setLoading(false)
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result?.token ?: ""
                            goToAdminDashboard(token)
                        } else {
                            b.tvError.text = "Gagal mengambil token sesi"
                            b.tvError.visibility = View.VISIBLE
                        }
                    }
                } else {
                    setLoading(false)
                    b.tvError.text = "Login Gagal: Email atau Password salah"
                    b.tvError.visibility = View.VISIBLE
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        b.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        b.btnLogin.isEnabled = !loading
    }

    private fun goToAdminDashboard(token: String) {
        val prefs = getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("AUTH_TOKEN", token).apply()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("BUKA_ADMIN", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}