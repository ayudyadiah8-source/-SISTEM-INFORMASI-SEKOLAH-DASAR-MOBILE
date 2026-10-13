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
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import hapsari.ayudyadiah.sdnpojok2app.databinding.FragmentStatusBinding
import org.json.JSONException
import org.json.JSONObject

/**
 * Menampilkan riwayat status daftar ulang dari API Laravel.
 */
class FragmentStatus : Fragment() {

    private var _b: FragmentStatusBinding? = null
    private val b get() = _b!!
    private val daftarStatus = mutableListOf<String>()

    private lateinit var adapter: ArrayAdapter<String>
    private var requestQueue: RequestQueue? = null

    companion object {
        // Disamakan dengan FragmentDaftarUlang.kt: IP:PORT artisan serve
        private const val API_URL = "http://192.168.22.105:8000/api/pendaftaran"
    }

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
        requestQueue?.cancelAll(this)
        requestQueue = null
        super.onDestroyView()
        _b = null
    }
}