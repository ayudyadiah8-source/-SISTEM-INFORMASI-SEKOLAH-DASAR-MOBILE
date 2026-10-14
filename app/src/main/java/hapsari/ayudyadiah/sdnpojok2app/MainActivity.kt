package hapsari.ayudyadiah.sdnpojok2app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessaging
import hapsari.ayudyadiah.sdnpojok2app.admin.AdminDashboardFragment
import hapsari.ayudyadiah.sdnpojok2app.databinding.ActivityMainBinding
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentBeranda
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentDaftarUlang
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentProfil
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentStatus

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // 1. Meminta izin notifikasi (Khusus Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // 2. Mengambil FCM Token perangkat
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN_BARU", "Token FCM Saya: $token")
            } else {
                Log.w("FCM_TOKEN_BARU", "Gagal mengambil token FCM", task.exception)
            }
        }

        // 3. CEK STATUS ADMIN
        val prefs = getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("AUTH_TOKEN", "")
        isAdmin = intent.getBooleanExtra("BUKA_ADMIN", false) || !token.isNullOrEmpty()

        // 4. ATUR TAMPILAN BOTTOM NAVIGATION
        val menu = b.bottomNav.menu
        val menuBeranda = menu.findItem(R.id.navBeranda)
        val menuDaftarUlang = menu.findItem(R.id.navDaftarUlang)
        val menuProfil = menu.findItem(R.id.navProfil)

        if (isAdmin) {
            // Tampilan jika yang masuk adalah Admin
            menuBeranda.title = "Dashboard"
            menuDaftarUlang.isVisible = false // Sembunyikan Daftar Ulang untuk Admin
            menuProfil.isVisible = true

            if (savedInstanceState == null) {
                openFragment(AdminDashboardFragment())
            }
        } else {
            // Tampilan jika yang masuk adalah Publik biasa
            menuBeranda.title = "Beranda"
            menuDaftarUlang.isVisible = true
            menuProfil.isVisible = false

            if (savedInstanceState == null) {
                openFragment(FragmentBeranda())
            }
        }

        // 5. MENGATUR KLIK BOTTOM NAVIGATION
        b.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment? = when (item.itemId) {
                R.id.navBeranda -> {
                    if (isAdmin) AdminDashboardFragment() else FragmentBeranda()
                }
                R.id.navDaftarUlang -> {
                    if (isAdmin) null else FragmentDaftarUlang()
                }
                R.id.navStatus -> FragmentStatus()
                R.id.navProfil -> {
                    if (isAdmin) FragmentProfil() else null
                }
                else -> {
                    if (isAdmin) AdminDashboardFragment() else FragmentBeranda()
                }
            }

            if (fragment != null) {
                openFragment(fragment)
                true
            } else {
                false
            }
        }
    }

    // Fungsi untuk mengganti fragment di dalam container
    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(b.fragmentContainer.id, fragment)
            .commit()
    }
}