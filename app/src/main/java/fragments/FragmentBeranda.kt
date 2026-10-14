package hapsari.ayudyadiah.sdnpojok2app.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import hapsari.ayudyadiah.sdnpojok2app.LoginActivity
import hapsari.ayudyadiah.sdnpojok2app.R
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

        // 1. Pintu Rahasia ke Login Admin
        b.tvPintuAdmin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }

        // 2. Setup Video Sambutan
        setupVideoSambutan()

        // 3. Tombol Map
        b.btnLihatMap.setOnClickListener {
            bukaLokasiSekolah()
        }
    }

    private fun setupVideoSambutan() {
        // Alamat resource video di res/raw/video_sekolah.mp4
        val videoPath = "android.resource://${requireContext().packageName}/${R.raw.video_sekolah}"
        val uri = Uri.parse(videoPath)
        b.videoSambutan.setVideoURI(uri)

        // Menambahkan kontrol standar (Play, Pause, Progress) saat video diklik
        val mediaController = MediaController(requireContext())
        mediaController.setAnchorView(b.videoSambutan)
        b.videoSambutan.setMediaController(mediaController)

        // Aksi Tombol Putar Video
        b.btnPutarVideo.setOnClickListener {
            if (b.videoSambutan.isPlaying) {
                b.videoSambutan.pause()
                b.btnPutarVideo.text = "Putar Video Sambutan"
            } else {
                b.videoSambutan.start()
                b.btnPutarVideo.text = "Pause Video Sambutan"
            }
        }

        // Reset teks tombol jika video selesai diputar
        b.videoSambutan.setOnCompletionListener {
            b.btnPutarVideo.text = "Putar Video Sambutan"
        }
        
        // Memastikan video siap (opsional: bisa langsung play di sini jika mau)
        b.videoSambutan.requestFocus()
    }

    private fun bukaLokasiSekolah() {
        val latitude = -7.801640
        val longitude = 111.986870
        val label = "SDN Pojok 2 Kota Kediri"
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)

        if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}