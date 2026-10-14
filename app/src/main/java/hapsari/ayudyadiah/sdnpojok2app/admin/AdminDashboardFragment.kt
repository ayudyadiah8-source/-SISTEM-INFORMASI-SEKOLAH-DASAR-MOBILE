package hapsari.ayudyadiah.sdnpojok2app.admin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import hapsari.ayudyadiah.sdnpojok2app.MainActivity
import hapsari.ayudyadiah.sdnpojok2app.R
import org.json.JSONException
import org.json.JSONObject

class AdminDashboardFragment : Fragment() {

    private lateinit var recyclerPendaftaran: RecyclerView
    private lateinit var tvTotalPendaftar: TextView
    private lateinit var tvMenungguVerifikasi: TextView
    private lateinit var btnLogoutAdmin: ImageButton

    private lateinit var adminAdapter: AdminAdapter
    private val daftarData = mutableListOf<JSONObject>()
    private var requestQueue: RequestQueue? = null

    companion object {
        private const val BASE_URL = "http://192.168.40.69:8000/api"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.admin_dashboard, container, false)
        recyclerPendaftaran = view.findViewById(R.id.recyclerPendaftaran)
        tvTotalPendaftar = view.findViewById(R.id.tvTotalPendaftar)
        tvMenungguVerifikasi = view.findViewById(R.id.tvMenungguVerifikasi)
        btnLogoutAdmin = view.findViewById(R.id.btnLogoutAdmin)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestQueue = Volley.newRequestQueue(requireContext())

        recyclerPendaftaran.layoutManager = LinearLayoutManager(requireContext())
        adminAdapter = AdminAdapter(daftarData) { id, statusBaru ->
            kirimValidasiStatus(id, statusBaru)
        }
        recyclerPendaftaran.adapter = adminAdapter

        btnLogoutAdmin.setOnClickListener { prosesLogout() }
        loadDataPendaftaran()
    }

    private fun loadDataPendaftaran() {
        val url = "$BASE_URL/pendaftaran"
        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val dataArray = jsonResponse.getJSONArray("data")
                    daftarData.clear()
                    var jumlahMenunggu = 0

                    for (i in 0 until dataArray.length()) {
                        val obj = dataArray.getJSONObject(i)
                        daftarData.add(obj)
                        val du = obj.optJSONObject("daftar_ulang")
                        val status = (du?.optString("status") ?: obj.optString("status") ?: "").trim()
                        if (status.contains("Menunggu", true) || status.isEmpty()) {
                            jumlahMenunggu++
                        }
                    }

                    tvTotalPendaftar.text = daftarData.size.toString()
                    tvMenungguVerifikasi.text = jumlahMenunggu.toString()
                    adminAdapter.updateData(daftarData)
                } catch (e: JSONException) {
                    Log.e("ADMIN_API", "Error parsing: ${e.message}")
                }
            },
            { showToast("Gagal memuat data. Periksa IP Server.") }
        )
        requestQueue?.add(request)
    }

    private fun kirimValidasiStatus(idPendaftaran: String, statusBaru: String) {
        val url = "$BASE_URL/pendaftaran/$idPendaftaran/validasi"
        val prefs = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        val token = prefs.getString("AUTH_TOKEN", "") ?: ""

        val request = object : StringRequest(
            Request.Method.POST, url,
            { 
                showToast("Berhasil: Pendaftaran $statusBaru")
                loadDataPendaftaran() 
            },
            { error ->
                val response = error.networkResponse
                val statusCode = response?.statusCode ?: 0
                
                // MENGAMBIL PESAN ERROR ASLI DARI LARAVEL (Untuk debug 500)
                val errorBody = if (response?.data != null) {
                    try {
                        val jsonErr = JSONObject(String(response.data))
                        jsonErr.optString("message", "Error $statusCode")
                    } catch (e: Exception) {
                        String(response.data)
                    }
                } else {
                    "Gagal terhubung ke server"
                }
                
                Log.e("ADMIN_DEBUG", "Error $statusCode: $errorBody")
                showToast("Gagal ($statusCode): $errorBody")
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf("status" to statusBaru)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Accept"] = "application/json"
                return headers
            }
        }

        request.retryPolicy = DefaultRetryPolicy(20000, 0, 1f)
        requestQueue?.add(request)
    }

    private fun prosesLogout() {
        FirebaseAuth.getInstance().signOut()
        val prefs = requireContext().getSharedPreferences("AdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showToast(msg: String) {
        if (isAdded) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}