package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

    private val baseUrl = "http://192.168.40.69:8000/api/pendaftaran"

    // Konfigurasi Supabase Anda
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

    private val galeriLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (activeBerkasCode == 1) {
                berkas1Uri = uri
                b.tvBerkas1.text = "File terpilih: ${uri.lastPathSegment}"
            } else {
                berkas2Uri = uri
                b.tvBerkas2.text = "File terpilih: ${uri.lastPathSegment}"
            }
        }
    }

    private val kameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { berhasil ->
        if (berhasil) {
            if (activeBerkasCode == 1) {
                berkas1Uri = fotoKameraUri
                b.tvBerkas1.text = "Foto kamera tersimpan"
            } else {
                berkas2Uri = fotoKameraUri
                b.tvBerkas2.text = "Foto kamera tersimpan"
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) bukaKamera() else Toast.makeText(requireContext(), "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentDaftarUlangBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinnerKelas()
        setupAutoCompleteAsalSekolah()
        setupPickers()

        b.btnGaleri1.setOnClickListener { activeBerkasCode = 1; galeriLauncher.launch("image/*") }
        b.btnKamera1.setOnClickListener { activeBerkasCode = 1; cekIzinLaluBukaKamera() }
        b.btnGaleri2.setOnClickListener { activeBerkasCode = 2; galeriLauncher.launch("image/*") }
        b.btnKamera2.setOnClickListener { activeBerkasCode = 2; cekIzinLaluBukaKamera() }

        b.btnKirim.setOnClickListener { kirimFormulir() }
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
        val file = File.createTempFile("berkas_${activeBerkasCode}_", ".jpg", requireContext().cacheDir)
        fotoKameraUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        kameraLauncher.launch(fotoKameraUri!!)
    }

    // Fungsi suspend untuk mengunggah file Uri ke Supabase Storage
    private suspend fun uploadKeSupabase(uri: Uri, folderName: String): String? {
        return try {
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val fileName = "$folderName/berkas_${System.currentTimeMillis()}.jpg"

            // Upload ke bucket "dokumen-registrasi"
            supabase.storage.from("dokumen-registrasi").upload(fileName, bytes)

            // Mengembalikan Public URL dari file tersebut
            supabase.storage.from("dokumen-registrasi").publicUrl(fileName)
        } catch (e: Exception) {
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

        if (noPendaftaran.isEmpty() || nisn.isEmpty() || namaSiswa.isEmpty() || namaWali.isEmpty() || noWa.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi semua field data pendaftaran", Toast.LENGTH_SHORT).show()
            return
        }
        if (!b.cbSetuju.isChecked) {
            Toast.makeText(requireContext(), "Centang persetujuan data terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        b.progressUpload.visibility = View.VISIBLE

        // Menjalankan proses Coroutine untuk upload ke Supabase terlebih dahulu
        viewLifecycleOwner.lifecycleScope.launch {
            val urlAkta = berkas1Uri?.let { uploadKeSupabase(it, "akta") } ?: ""
            val urlKk = berkas2Uri?.let { uploadKeSupabase(it, "kk") } ?: ""

            // 1. Simpan data juga ke Firebase Firestore secara lokal/langsung
            simpanKeFirestore(
                noPendaftaran, nisn, namaSiswa, tanggalLahir, kelas,
                asalSekolah, namaWali, noWa, tglVerifikasi, jamVerifikasi, urlAkta, urlKk
            )

            // 2. Kirim data teks + URL string ke Laravel via StringRequest
            kirimDataKeLaravel(
                noPendaftaran, nisn, namaSiswa, tanggalLahir, kelas,
                asalSekolah, namaWali, noWa, tglVerifikasi, jamVerifikasi, urlAkta, urlKk
            )
        }
    }

    private fun simpanKeFirestore(
        noPendaftaran: String, nisn: String, namaSiswa: String, tglLahir: String,
        jenjang: String, asalSekolah: String, namaWali: String, noWa: String,
        tglVerifikasi: String, jamVerifikasi: String, urlAkta: String, urlKk: String
    ) {
        val db = FirebaseFirestore.getInstance()

        val dataPendaftaran = hashMapOf(
            "no_pendaftaran" to noPendaftaran,
            "nisn" to nisn,
            "nama_siswa" to namaSiswa,
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

        // Menyimpan data ke koleksi "pendaftaran_siswa" di Firestore
        db.collection("pendaftaran_siswa")
            .document(noPendaftaran) // Menggunakan nomor pendaftaran sebagai ID dokumen unik
            .set(dataPendaftaran)
            .addOnSuccessListener {
                Log.d("FIRESTORE", "Data berhasil disimpan ke Firestore")
            }
            .addOnFailureListener { e ->
                Log.w("FIRESTORE", "Gagal menyimpan ke Firestore", e)
            }
    }

    private fun kirimDataKeLaravel(
        noPendaftaran: String, nisn: String, namaSiswa: String, tglLahir: String,
        jenjang: String, asalSekolah: String, namaWali: String, noWa: String,
        tglVerifikasi: String, jamVerifikasi: String, urlAkta: String, urlKk: String
    ) {
        val request = object : StringRequest(
            Request.Method.POST, baseUrl,
            Response.Listener { response ->
                b.progressUpload.visibility = View.GONE
                Toast.makeText(requireContext(), "Pendaftaran, Supabase & Firestore Berhasil!", Toast.LENGTH_LONG).show()
            },
            Response.ErrorListener { error ->
                b.progressUpload.visibility = View.GONE
                val detail = error.networkResponse?.let { "HTTP ${it.statusCode}: ${String(it.data)}" } ?: error.toString()
                Toast.makeText(requireContext(), "Gagal terhubung ke Laravel: $detail", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): Map<String, String> = mapOf(
                "no_pendaftaran" to noPendaftaran,
                "nisn" to nisn,
                "nama_siswa" to namaSiswa,
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