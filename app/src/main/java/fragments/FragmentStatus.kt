package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import hapsari.ayudyadiah.sdnpojok2app.R
import hapsari.ayudyadiah.sdnpojok2app.databinding.FragmentStatusBinding
import org.json.JSONException
import org.json.JSONObject

/**
 * Menampilkan riwayat status daftar ulang dari API Laravel.
 * Mengimplementasikan fitur:
 * 9. ListView
 * 10. OptionsMenu
 * 11. ContextMenu
 * 12. PopupMenu
 */
class FragmentStatus : Fragment() {

    private var _b: FragmentStatusBinding? = null
    private val b get() = _b!!
    private val daftarStatus = mutableListOf<String>()

    private lateinit var adapter: ArrayAdapter<String>
    private var requestQueue: RequestQueue? = null

    companion object {
        // Update IP sesuai konfigurasi jaringan user
        private const val API_URL = "http://192.168.41.113:8000/api/pendaftaran"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aktifkan OptionsMenu di Fragment
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentStatusBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // FITUR 9: ListView
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, daftarStatus)
        b.listStatus.adapter = adapter

        // FITUR 11: Daftarkan ListView agar bisa menampilkan ContextMenu saat ditekan lama
        registerForContextMenu(b.listStatus)

        // FITUR 12: PopupMenu saat ImageButton diklik
        b.btnMenuPopup.setOnClickListener { view ->
            showPopupMenu(view)
        }

        requestQueue = Volley.newRequestQueue(requireContext())
        loadPendaftaran()
    }

    private fun loadPendaftaran() {
        val request = StringRequest(
            Request.Method.GET,
            API_URL,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (!jsonResponse.optBoolean("success")) {
                        showToast(jsonResponse.optString("message", "Gagal memuat data pendaftaran."))
                        return@StringRequest
                    }

                    val pendaftaran = jsonResponse.getJSONArray("data")
                    daftarStatus.clear()

                    for (index in 0 until pendaftaran.length()) {
                        val dataItem = pendaftaran.getJSONObject(index)
                        val namaSiswa = dataItem.optString("nama_siswa", "Nama tidak tersedia")
                        val statusDaftarUlang = dataItem.optJSONObject("daftar_ulang")
                            ?.optString("status")
                            ?.takeIf { it.isNotBlank() }
                            ?: "Belum ada status"

                        daftarStatus.add("$namaSiswa\n$statusDaftarUlang")
                    }

                    adapter.notifyDataSetChanged()
                    showToast("Data berhasil dimuat!")
                } catch (exception: JSONException) {
                    showToast("Gagal parsing data: ${exception.message ?: "format JSON tidak valid"}")
                }
            },
            { error ->
                showToast("Koneksi gagal: ${error.localizedMessage ?: "tidak dapat menghubungi server"}")
            }
        ).apply {
            tag = this@FragmentStatus
        }

        requestQueue?.add(request)
    }

    private fun showToast(message: String) {
        context?.let { fragmentContext ->
            Toast.makeText(fragmentContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    // FITUR 10: OptionsMenu (Menu di pojok kanan atas Toolbar)
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(Menu.NONE, 100, Menu.NONE, "Refresh Data")
        menu.add(Menu.NONE, 101, Menu.NONE, "Bantuan")
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            100 -> {
                loadPendaftaran()
                true
            }
            101 -> {
                showToast("Hubungi admin sekolah jika data tidak muncul.")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // FITUR 11: ContextMenu (Muncul saat ListView ditekan lama)
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu.setHeaderTitle("Pilih Aksi")
        menu.add(0, 1, 0, "Lihat Detail")
        menu.add(0, 2, 1, "Hapus dari Riwayat")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as? AdapterView.AdapterContextMenuInfo ?: return super.onContextItemSelected(item)
        val posisi = info.position
        val dataTerpilih = if (posisi < daftarStatus.size) daftarStatus[posisi] else ""

        return when (item.itemId) {
            1 -> {
                Toast.makeText(requireContext(), "Detail: $dataTerpilih", Toast.LENGTH_SHORT).show()
                true
            }
            2 -> {
                if (posisi < daftarStatus.size) {
                    daftarStatus.removeAt(posisi)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(requireContext(), "Dihapus dari riwayat lokal", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // FITUR 12: PopupMenu (Muncul saat tombol titik tiga/menu diklik)
    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menu.add("Urutkan Berdasarkan Nama")
        popup.menu.add("Urutkan Berdasarkan Status")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Urutkan Berdasarkan Nama" -> {
                    daftarStatus.sort()
                    adapter.notifyDataSetChanged()
                    showToast("Diurutkan berdasarkan nama")
                    true
                }
                "Urutkan Berdasarkan Status" -> {
                    daftarStatus.sortBy { it.substringAfter("\n") }
                    adapter.notifyDataSetChanged()
                    showToast("Diurutkan berdasarkan status")
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onDestroyView() {
        requestQueue?.cancelAll(this)
        requestQueue = null
        super.onDestroyView()
        _b = null
    }
}