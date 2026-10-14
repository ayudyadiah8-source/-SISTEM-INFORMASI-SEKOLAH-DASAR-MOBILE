package hapsari.ayudyadiah.sdnpojok2app.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import hapsari.ayudyadiah.sdnpojok2app.R
import org.json.JSONObject

class AdminAdapter(
    private var daftarPendaftar: List<JSONObject>,
    private val onAksiKlik: (String, String) -> Unit
) : RecyclerView.Adapter<AdminAdapter.AdminViewHolder>() {

    class AdminViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNama: TextView = view.findViewById(R.id.tvNamaSiswaAdmin)
        val tvNisn: TextView = view.findViewById(R.id.tvNisnAdmin)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusValidasi)
        val btnSetujui: Button = view.findViewById(R.id.btnSetujui)
        val btnTolak: Button = view.findViewById(R.id.btnTolak)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.admin_item_pendaftaran, parent, false)
        return AdminViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminViewHolder, position: Int) {
        val data = daftarPendaftar[position]
        
        // Ambil ID primer (SiswaPpdb ID)
        val id = data.optString("id")
        val nama = data.optString("nama_siswa", "Tanpa Nama")
        val nisn = data.optString("nisn", "-")

        // Ambil status dari relasi (Laravel biasanya merubah camelCase ke snake_case di JSON)
        val du = data.optJSONObject("daftar_ulang") ?: data.optJSONObject("daftarUlang")
        val status = (du?.optString("status") ?: "Menunggu Verifikasi").trim()

        holder.tvNama.text = nama
        holder.tvNisn.text = "NISN: $nisn"
        holder.tvStatus.text = status

        // 1. Pewarnaan Status
        val isAccepted = status.contains("Setuju", true) || status.contains("Terima", true)
        val isRejected = status.contains("Tolak", true)

        when {
            isAccepted -> holder.tvStatus.setTextColor(Color.parseColor("#059669")) // Hijau
            isRejected -> holder.tvStatus.setTextColor(Color.parseColor("#DC2626")) // Merah
            else -> holder.tvStatus.setTextColor(Color.parseColor("#D97706")) // Oranye
        }

        // 2. LOGIKA TOMBOL: HANYA muncul jika status masih "Menunggu Verifikasi"
        if (status.contains("Menunggu", true) && !isAccepted && !isRejected) {
            holder.btnSetujui.visibility = View.VISIBLE
            holder.btnTolak.visibility = View.VISIBLE
        } else {
            holder.btnSetujui.visibility = View.GONE
            holder.btnTolak.visibility = View.GONE
        }

        // Gunakan kata status yang Anda inginkan disimpan di Database
        holder.btnSetujui.setOnClickListener { onAksiKlik(id, "Disetujui") }
        holder.btnTolak.setOnClickListener { onAksiKlik(id, "Ditolak") }
    }

    override fun getItemCount(): Int = daftarPendaftar.size

    fun updateData(newData: List<JSONObject>) {
        this.daftarPendaftar = newData
        notifyDataSetChanged()
    }
}