package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.FirebaseFirestore
import hapsari.ayudyadiah.sdnpojok2app.databinding.FragmentDaftarUlangBinding
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FragmentDaftarUlang : Fragment() {

    private var _b: FragmentDaftarUlangBinding? = null
    private val b get() = _b!!

    // UPDATE: IP disesuaikan ke 192.168.100.6
    private val baseUrl = "http://192.168.41.113:8000/api/pendaftaran"

    private val supabase = createSupabaseClient(
        supabaseUrl = "https://ufbxmwdnxlzgfrjwwshd.supabase.co",
        supabaseKey = "sb_publishable_coq6yyI1j3qx3vOIop3Syg_a4P7IPlb"
    ) {
        install(Storage)
    }

    private var berkas1Uri: Uri? = null   // Akta Kelahiran
    private var berkas2Uri: Uri? = null   // Kartu Keluarga
    private var fotoKameraUri: Uri? = null
    private var activeBerkasCode = 1      // 1 = Akta, 2 = KK

    private val galeriLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val namaFile = getFileName(it)
            if (activeBerkasCode == 1) {
                berkas1Uri = it
                b.tvBerkas1.text = "Terpilih: $namaFile"
                b.tvBerkas1.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                berkas2Uri = it
                b.tvBerkas2.text = "Terpilih: $namaFile"
                b.tvBerkas2.setTextColor(Color.parseColor("#2E7D32"))
            }
        }
    }

    private val kameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { berhasil ->
        if (berhasil && fotoKameraUri != null) {
            if (activeBerkasCode == 1) {
                berkas1Uri = fotoKameraUri
                b.tvBerkas1.text = "Foto Kamera Berhasil"
                b.tvBerkas1.setTextColor(Color.parseColor("#2E7D32"))
            } else {
                berkas2Uri = fotoKameraUri
                b.tvBerkas2.text = "Foto Kamera Berhasil"
                b.tvBerkas2.setTextColor(Color.parseColor("#2E7D32"))
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) bukaKamera() else Toast.makeText(requireContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let {
            activeBerkasCode = it.getInt("activeBerkasCode", 1)
            berkas1Uri = it.getParcelable("berkas1Uri")
            berkas2Uri = it.getParcelable("berkas2Uri")
            fotoKameraUri = it.getParcelable("fotoKameraUri")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("activeBerkasCode", activeBerkasCode)
        outState.putParcelable("berkas1Uri", berkas1Uri)
        outState.putParcelable("berkas2Uri", berkas2Uri)
        outState.putParcelable("fotoKameraUri", fotoKameraUri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentDaftarUlangBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        berkas1Uri?.let { 
            b.tvBerkas1.text = "Berkas sudah siap" 
            b.tvBerkas1.setTextColor(Color.parseColor("#2E7D32"))
        }
        berkas2Uri?.let { 
            b.tvBerkas2.text = "Berkas sudah siap"
            b.tvBerkas2.setTextColor(Color.parseColor("#2E7D32"))
        }

        setupSpinnerKelas()
        setupAutoCompleteAsalSekolah()
        setupPickers()

        b.btnGaleri1.setOnClickListener { activeBerkasCode = 1; galeriLauncher.launch("image/*") }
        b.btnKamera1.setOnClickListener { activeBerkasCode = 1; cekIzinLaluBukaKamera() }
        b.btnGaleri2.setOnClickListener { activeBerkasCode = 2; galeriLauncher.launch("image/*") }
        b.btnKamera2.setOnClickListener { activeBerkasCode = 2; cekIzinLaluBukaKamera() }

        b.btnKirim.setOnClickListener { kirimFormulir() }
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "file_unknown"
    }

    private fun setupSpinnerKelas() {
        val daftarKelas = listOf("Kelas 1", "Kelas 2", "Kelas 3", "Kelas 4", "Kelas 5", "Kelas 6")
        b.spinnerKelas.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, daftarKelas
        )
    }

    private fun setupAutoCompleteAsalSekolah() {
        val daftarSekolah = listOf(
            "TK Dharma Wanita Kediri", "TK Pembina Kediri", "TK Aisyiyah Kediri", "TK Kartika Kediri"
        )
        b.autoAsalSekolah.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, daftarSekolah)
        )
        b.autoAsalSekolah.threshold = 1
    }

    private fun setupPickers() {
        val kalender = Calendar.getInstance()

        b.edTanggalLahir.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("id-ID"))
                    kalender.set(year, month, day)
                    b.edTanggalLahir.setText(format.format(kalender.time))
                },
                kalender.get(Calendar.YEAR) - 7, kalender.get(Calendar.MONTH), kalender.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        b.edTanggalVerifikasi.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.forLanguageTag("id-ID"))
                    kalender.set(year, month, day)
                    b.edTanggalVerifikasi.setText(format.format(kalender.time))
                },
                kalender.get(Calendar.YEAR), kalender.get(Calendar.MONTH), kalender.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        b.edJamVerifikasi.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    b.edJamVerifikasi.setText(String.format(Locale.forLanguageTag("id-ID"), "%02d:%02d", hour, minute))
                },
                kalender.get(Calendar.HOUR_OF_DAY), kalender.get(Calendar.MINUTE), true
            ).show()
        }
    }

    private fun cekIzinLaluBukaKamera() {
        val izin = android.Manifest.permission.CAMERA
        val sudahDiizinkan = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), izin
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (sudahDiizinkan) bukaKamera() else permissionLauncher.launch(izin)
    }

    private fun bukaKamera() {
        val fileName = "camera_${System.currentTimeMillis()}.jpg"
        val file = File(requireContext().cacheDir, fileName)
        fotoKameraUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        kameraLauncher.launch(fotoKameraUri!!)
    }

    private suspend fun uploadKeSupabase(uri: Uri, folderName: String): String? {
        return try {
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val fileName = "$folderName/berkas_${System.currentTimeMillis()}.jpg"

            supabase.storage.from("dokumen-registrasi").upload(fileName, bytes)
            supabase.storage.from("dokumen-registrasi").publicUrl(fileName)
        } catch (e: Exception) {
            Log.e("SUPABASE_UPLOAD", "Gagal: ${e.message}")
            null
        }
    }

    private fun kirimFormulir() {
        val noPendaftaran = b.edNoPendaftaran.text.toString().trim()
        val nisn = b.edNisn.text.toString().trim()
        val namaSiswa = b.edNamaSiswa.text.toString().trim()
        val tanggalLahir = b.edTanggalLahir.text.toString().trim()
        val kelas = b.spinnerKelas.selectedItem?.toString() ?: ""
        val asalSekolah = b.autoAsalSekolah.text.toString().trim()
        val namaWali = b.edNamaWali.text.toString().trim()
        val noWa = b.edNoWa.text.toString().trim()
        val tglVerifikasi = b.edTanggalVerifikasi.text.toString().trim()
        val jamVerifikasi = b.edJamVerifikasi.text.toString().trim()

        val selectedId = b.rgJenisKelamin.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Pilih jenis kelamin", Toast.LENGTH_SHORT).show()
            return
        }
        val rbSelected = b.root.findViewById<RadioButton>(selectedId)
        val jenisKelamin = rbSelected.text.toString()

        if (noPendaftaran.isEmpty() || nisn.isEmpty() || namaSiswa.isEmpty() || namaWali.isEmpty() || noWa.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi semua field data pendaftaran", Toast.LENGTH_SHORT).show()
            return
        }
        if (!b.cbSetuju.isChecked) {
            Toast.makeText(requireContext(), "Centang persetujuan data terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (berkas1Uri == null || berkas2Uri == null) {
            Toast.makeText(requireContext(), "Harap unggah kedua berkas (Akta & KK)", Toast.LENGTH_SHORT).show()
            return
        }

        b.progressUpload.visibility = View.VISIBLE
        b.btnKirim.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val urlAkta = berkas1Uri?.let { uploadKeSupabase(it, "akta") } ?: ""
            val urlKk = berkas2Uri?.let { uploadKeSupabase(it, "kk") } ?: ""

            if (urlAkta.isEmpty() || urlKk.isEmpty()) {
                b.progressUpload.visibility = View.GONE
                b.btnKirim.isEnabled = true
                Toast.makeText(requireContext(), "Gagal mengunggah berkas ke Supabase", Toast.LENGTH_SHORT).show()
                return@launch
            }

            simpanKeFirestore(
                noPendaftaran, nisn, namaSiswa, jenisKelamin, tanggalLahir, kelas,
                asalSekolah, namaWali, noWa, tglVerifikasi, jamVerifikasi, urlAkta, urlKk
            )

            kirimDataKeLaravel(
                noPendaftaran, nisn, namaSiswa, jenisKelamin, tanggalLahir, kelas,
                asalSekolah, namaWali, noWa, tglVerifikasi, jamVerifikasi, urlAkta, urlKk
            )
        }
    }

    private fun simpanKeFirestore(
        noPendaftaran: String, nisn: String, namaSiswa: String, jenisKelamin: String, tglLahir: String,
        jenjang: String, asalSekolah: String, namaWali: String, noWa: String,
        tglVerifikasi: String, jamVerifikasi: String, urlAkta: String, urlKk: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val dataPendaftaran = hashMapOf(
            "no_pendaftaran" to noPendaftaran,
            "nisn" to nisn,
            "nama_siswa" to namaSiswa,
            "jenis_kelamin" to jenisKelamin,
            "tanggal_lahir" to tglLahir,
            "jenjang" to jenjang,
            "asal_sekolah" to asalSekolah,
            "nama_wali" to namaWali,
            "no_whatsapp" to noWa,
            "tanggal_verifikasi" to tglVerifikasi,
            "jam_verifikasi" to jamVerifikasi,
            "file_akta" to urlAkta,
            "file_kk" to urlKk,
            "status" to "Menunggu Verifikasi",
            "created_at" to System.currentTimeMillis()
        )

        db.collection("pendaftaran_siswa").document(noPendaftaran).set(dataPendaftaran)
    }

    private fun kirimDataKeLaravel(
        noPendaftaran: String, nisn: String, namaSiswa: String, jenisKelamin: String, tglLahir: String,
        jenjang: String, asalSekolah: String, namaWali: String, noWa: String,
        tglVerifikasi: String, jamVerifikasi: String, urlAkta: String, urlKk: String
    ) {
        val request = object : StringRequest(
            Request.Method.POST, baseUrl,
            Response.Listener {
                b.progressUpload.visibility = View.GONE
                b.btnKirim.isEnabled = true
                Toast.makeText(requireContext(), "Pendaftaran Berhasil Terkirim ke Semua!", Toast.LENGTH_LONG).show()
            },
            Response.ErrorListener { error ->
                b.progressUpload.visibility = View.GONE
                b.btnKirim.isEnabled = true
                val errorMsg = error.networkResponse?.let { "HTTP ${it.statusCode}" } ?: error.toString()
                Toast.makeText(requireContext(), "Gagal ke Laravel ($errorMsg). Data sudah tersimpan di Firebase.", Toast.LENGTH_LONG).show()
                Log.e("LARAVEL_ERROR", "Detail: $error")
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf(
                "no_pendaftaran" to noPendaftaran,
                "nisn" to nisn,
                "nama_siswa" to namaSiswa,
                "jenis_kelamin" to jenisKelamin,
                "tanggal_lahir" to tglLahir,
                "jenjang" to jenjang,
                "asal_sekolah" to asalSekolah,
                "nama_wali" to namaWali,
                "no_whatsapp" to noWa,
                "tanggal_verifikasi" to tglVerifikasi,
                "jam_verifikasi" to jamVerifikasi,
                "file_akta" to urlAkta,
                "file_kk" to urlKk
            )
        }
        Volley.newRequestQueue(requireContext()).add(request)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}