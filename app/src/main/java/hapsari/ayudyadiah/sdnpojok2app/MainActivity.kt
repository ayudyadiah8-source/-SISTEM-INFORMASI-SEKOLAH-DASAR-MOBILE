package hapsari.ayudyadiah.sdnpojok2app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.messaging.FirebaseMessaging
import hapsari.ayudyadiah.sdnpojok2app.databinding.ActivityMainBinding
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentBeranda
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentDaftarUlang
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentProfil
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentStatus

/**
- Container utama: menampung BottomNavigationView dan mengganti Fragment

- di dalam fragmentContainer sesuai menu yang dipilih.

 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Meminta izin notifikasi secara runtime (Khusus Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Mengambil FCM Token perangkat saat MainActivity dibuka
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN_BARU", "Token FCM Saya: $token")
            } else {
                Log.w("FCM_TOKEN_BARU", "Gagal mengambil token FCM", task.exception)
            }
        }

        // Tampilan awal: Beranda
        if (savedInstanceState == null) {
            openFragment(FragmentBeranda())
        }

        b.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.navBeranda -> FragmentBeranda()
                R.id.navDaftarUlang -> FragmentDaftarUlang()
                R.id.navStatus -> FragmentStatus()
                R.id.navProfil -> FragmentProfil()
                else -> FragmentBeranda()
            }
            openFragment(fragment)
            true
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(b.fragmentContainer.id, fragment)
            .commit()
    }
}