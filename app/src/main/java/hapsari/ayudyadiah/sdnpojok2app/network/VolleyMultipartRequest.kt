package hapsari.ayudyadiah.sdnpojok2app.network

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Request kustom untuk mengirim multipart/form-data (teks + file) lewat Volley,
 * karena StringRequest bawaan Volley tidak bisa mengirim file sama sekali.
 *
 * Cara pakai: lihat contoh di FragmentDaftarUlang.kt bagian kirimFormulir().
 */
abstract class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val errorListener: Response.ErrorListener,
    private val listener: Response.Listener<NetworkResponse>
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "apiclient-${System.currentTimeMillis()}"
    private val mimeTypeMultipart = "multipart/form-data;boundary=$boundary"

    /** Field teks biasa, mis. {"nama_siswa": "Budi"}. Override kalau ada data teks. */
    override fun getParams(): Map<String, String> = emptyMap()

    /** Field file, mis. {"file_akta": DataPart(nama, bytes, mimeType)}. Wajib di-override. */
    abstract fun getByteData(): Map<String, DataPart>

    override fun getBodyContentType(): String = mimeTypeMultipart

    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // Bagian teks
            for ((key, value) in getParams()) {
                buildTextPart(dos, key, value)
            }
            // Bagian file
            for ((key, dataPart) in getByteData()) {
                buildFilePart(dos, key, dataPart)
            }
            // Penutup
            dos.writeBytes("--$boundary--\r\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bos.toByteArray()
    }

    private fun buildTextPart(dos: DataOutputStream, name: String, value: String) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        dos.writeBytes("$value\r\n")
    }

    private fun buildFilePart(dos: DataOutputStream, name: String, dataPart: DataPart) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes(
            "Content-Disposition: form-data; name=\"$name\"; filename=\"${dataPart.fileName}\"\r\n"
        )
        if (dataPart.type.isNotEmpty()) dos.writeBytes("Content-Type: ${dataPart.type}\r\n")
        dos.writeBytes("\r\n")
        dos.write(dataPart.content)
        dos.writeBytes("\r\n")
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return try {
            Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(ParseError(e))
        }
    }

    override fun deliverResponse(response: NetworkResponse) {
        listener.onResponse(response)
    }

    override fun deliverError(error: com.android.volley.VolleyError) {
        errorListener.onErrorResponse(error)
    }

    /** Satu bagian file: nama file, isi byte-nya, dan mime type (mis. "image/jpeg"). */
    data class DataPart(val fileName: String, val content: ByteArray, val type: String = "")
}