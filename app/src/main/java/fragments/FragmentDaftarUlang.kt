package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import hapsari.ayudyadiah.sdnpojok2app.databinding.FragmentDaftarUlangBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Formulir daftar ulang.
 * Catatan: tombol "Kirim" saat ini hanya validasi + Toast (belum terhubung ke API Laravel).
 * Upload berkas ke Supabase Storage akan ditambahkan di tahap selanjutnya.
 */
class FragmentDaftarUlang : Fragment() {

    private var _b: FragmentDaftarUlangBinding? = null
    private val b get() = _b!!

    private var berkasUri: Uri? = null
    private var fotoKameraUri: Uri? = null

    // Launcher untuk memilih foto dari Galeri
    private val galeriLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            berkasUri = uri
            b.tvNamaBerkas.text = "Berkas dipilih dari galeri"
        }
    }

    // Launcher untuk mengambil foto dari Kamera
    private val kameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { berhasil ->
        if (berhasil) {
            berkasUri = fotoKameraUri
            b.tvNamaBerkas.text = "Berkas diambil dari kamera"
        }
    }

    // Launcher untuk minta izin kamera (Android 6+)
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
        setupAutoCompleteSekolahAsal()
        setupDatePicker()

        b.btnGaleri.setOnClickListener { galeriLauncher.launch("image/*") }
        b.btnKamera.setOnClickListener { cekIzinLaluBukaKamera() }
        b.btnKirim.setOnClickListener { kirimFormulir() }
    }

    private fun setupSpinnerKelas() {
        val daftarKelas = listOf("Kelas 1", "Kelas 2", "Kelas 3", "Kelas 4", "Kelas 5", "Kelas 6")
        b.spinnerKelas.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, daftarKelas
        )
    }

    private fun setupAutoCompleteSekolahAsal() {
        val daftarSekolah = listOf(
            "SDN Pojok 1 Kota Kediri", "SDN Pojok 3 Kota Kediri",
            "SDN Campurejo 1", "SDN Campurejo 2", "SDN Jamsaren 1"
        )
        b.autoSekolahAsal.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, daftarSekolah)
        )
        b.autoSekolahAsal.threshold = 1
    }

    private fun setupDatePicker() {
        val kalender = Calendar.getInstance()
        b.edTanggalLahir.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                    kalender.set(year, month, day)
                    b.edTanggalLahir.setText(format.format(kalender.time))
                },
                kalender.get(Calendar.YEAR) - 7, // default sekitar usia SD
                kalender.get(Calendar.MONTH),
                kalender.get(Calendar.DAY_OF_MONTH)
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
        val file = File.createTempFile("berkas_", ".jpg", requireContext().cacheDir)
        fotoKameraUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", file
        )
        kameraLauncher.launch(fotoKameraUri!!)
    }

    private fun kirimFormulir() {
        val nama = b.edNamaSiswa.text.toString().trim()
        val tanggalLahir = b.edTanggalLahir.text.toString().trim()
        val namaWali = b.edNamaWali.text.toString().trim()
        val alamat = b.edAlamat.text.toString().trim()

        if (nama.isEmpty() || tanggalLahir.isEmpty() || namaWali.isEmpty() || alamat.isEmpty()) {
            Toast.makeText(requireContext(), "Lengkapi semua data wajib", Toast.LENGTH_SHORT).show()
            return
        }
        if (!b.cbSetuju.isChecked) {
            Toast.makeText(requireContext(), "Centang persetujuan data terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val jenisKelamin = if (b.rbLaki.isChecked) "Laki-laki" else "Perempuan"
        val kelas = b.spinnerKelas.selectedItem?.toString() ?: ""

        // TODO tahap selanjutnya: kirim data + berkasUri ke API Laravel, lalu upload berkas ke Supabase Storage
        Toast.makeText(
            requireContext(),
            "Formulir $nama ($jenisKelamin, $kelas) berhasil disimpan sementara",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}