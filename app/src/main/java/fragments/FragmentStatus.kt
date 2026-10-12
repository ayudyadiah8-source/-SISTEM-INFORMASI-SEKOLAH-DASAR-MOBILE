package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import hapsari.ayudyadiah.sdnpojok2app.databinding.FragmentStatusBinding

/**
 * Menampilkan riwayat status daftar ulang.
 * Saat ini memakai data dummy; nanti diganti dengan hasil panggilan API Laravel (Volley)
 * dan di-cache ke SQLite untuk mode offline.
 */
class FragmentStatus : Fragment() {

    private var _b: FragmentStatusBinding? = null
    private val b get() = _b!!

    // Data dummy sementara: "Nama Siswa - Status"
    private val daftarStatus = mutableListOf(
        "Budi Santoso - Menunggu Verifikasi",
        "Siti Aminah - Disetujui",
        "Rizky Pratama - Perlu Perbaikan Berkas"
    )

    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentStatusBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, daftarStatus)
        b.listStatus.adapter = adapter

        // Daftarkan ListView agar bisa menampilkan ContextMenu saat ditekan lama
        registerForContextMenu(b.listStatus)
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle("Pilih Aksi")
        menu.add(0, 1, 0, "Lihat Detail")
        menu.add(0, 2, 1, "Hapus dari Riwayat")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return super.onContextItemSelected(item)
        val posisi = info.position
        val dataTerpilih = daftarStatus[posisi]

        return when (item.itemId) {
            1 -> {
                Toast.makeText(requireContext(), "Detail: $dataTerpilih", Toast.LENGTH_SHORT).show()
                true
            }
            2 -> {
                daftarStatus.removeAt(posisi)
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Dihapus dari riwayat lokal", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}