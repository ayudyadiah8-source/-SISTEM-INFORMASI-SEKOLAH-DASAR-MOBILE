package hapsari.ayudyadiah.sdnpojok2app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import hapsari.ayudyadiah.sdnpojok2app.databinding.ActivityMainBinding
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentBeranda
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentDaftarUlang
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentProfil
import hapsari.ayudyadiah.sdnpojok2app.fragments.FragmentStatus

/**
 * Container utama: menampung BottomNavigationView dan mengganti Fragment
 * di dalam fragmentContainer sesuai menu yang dipilih.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

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