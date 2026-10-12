package hapsari.ayudyadiah.sdnpojok2app.fragments

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

        // Ganti URL ini dengan video profil sekolah yang sebenarnya (hosting sendiri / Supabase Storage)
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}