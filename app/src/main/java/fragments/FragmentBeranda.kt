package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.Toast
import androidx.fragment.app.Fragment
import hapsari.ayudyadiah.sdnpojok2app.databinding.FragmentBerandaBinding

class FragmentBeranda : Fragment() {

    private var _b: FragmentBerandaBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentBerandaBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ganti URL ini dengan video profil sekolah yang sebenarnya
        val urlVideo = "https://www.w3schools.com/html/mov_bbb.mp4"

        b.btnPutarVideo.setOnClickListener {
            try {
                val controller = MediaController(requireContext())
                b.videoSambutan.setMediaController(controller)
                controller.setAnchorView(b.videoSambutan)
                b.videoSambutan.setVideoURI(Uri.parse(urlVideo))
                b.videoSambutan.start()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal memuat video: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // FITUR 22: Integrasi Google Maps via Intent
        b.btnLihatMap.setOnClickListener {
            bukaLokasiSekolah()
        }
    }

    private fun bukaLokasiSekolah() {
        // Koordinat SDN Pojok 2 Kota Kediri (estimasi)
        val latitude = -7.801640
        val longitude = 111.986870
        val label = "SDN Pojok 2 Kota Kediri"
        
        // URI geo untuk membuka aplikasi peta
        val uri = "geo:$latitude,$longitude?q=$latitude,$longitude($label)"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        
        // Pastikan aplikasi peta tersedia
        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Jika tidak ada aplikasi peta, buka di browser
            val browserUri = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(browserUri)))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}