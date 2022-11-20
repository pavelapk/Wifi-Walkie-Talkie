package ru.pavelapk.wifi_walkie_talkie

import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class SocketHandler(private val socket: DatagramSocket, private val to: InetSocketAddress) {
    private var running: Boolean = false

    suspend fun run(messageListener: (msg: String) -> Unit) = withContext(Dispatchers.IO) {
        running = true

        val tasks = listOf(
            launch { recorder() },
            launch { player() }
        )
        tasks.joinAll()
    }

    private suspend fun recorder() = withContext(Dispatchers.IO) {
        val audioRecorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ) * 10
            )
        } catch (e: SecurityException) {
            shutdown()
            return@withContext
        }
        audioRecorder.startRecording()
        val buf = ByteArray(BUF_SIZE)
        while (running) {
            try {
                val bytesRead = audioRecorder.read(buf, 0, BUF_SIZE)
                if (bytesRead > 0) {
                    val packet = DatagramPacket(buf, bytesRead, to)
                    socket.send(packet)
                } else {
                    delay(SAMPLE_INTERVAL.toLong())
                }
            } catch (ex: Exception) {
                // TODO: Implement exception handling
                Log.e("SocketHandler", "recorder", ex)
                shutdown()
            } finally {

            }
        }
        audioRecorder.stop()
        audioRecorder.release()
    }

    private suspend fun player() = withContext(Dispatchers.IO) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(BUF_SIZE)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
        val buf = ByteArray(BUF_SIZE)
        while (running) {
            try {
                val packet = DatagramPacket(buf, BUF_SIZE)
                socket.receive(packet)
//                Log.i("SocketHandler", "Packet received: " + packet.length)
                track.write(packet.data, packet.offset, packet.length)
            } catch (ex: Exception) {
                // TODO: Implement exception handling
                Log.e("SocketHandler", "player", ex)
                shutdown()
            } finally {

            }
        }
        track.stop()
        track.flush()
        track.release()
    }

    private fun shutdown() {
        running = false
        socket.close()
        println("${to.address.hostAddress} closed the connection")
    }

    companion object {
        private const val SAMPLE_INTERVAL = 20 // Milliseconds
        private const val SAMPLE_SIZE = 2 // Bytes
        private const val SAMPLE_RATE = 16000
        private const val BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2 //Bytes

    }
}