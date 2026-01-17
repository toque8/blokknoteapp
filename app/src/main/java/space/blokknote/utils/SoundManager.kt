package space.blokknote.utils

import android.content.Context
import android.media.MediaPlayer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    private val context: Context
) {
    private var typingPlayer: MediaPlayer? = null
    private var erasePlayer: MediaPlayer? = null
    private var downloadPlayer: MediaPlayer? = null
    private var langPlayer: MediaPlayer? = null

    init {
        initializePlayers()
    }

    private fun initializePlayers() {
        typingPlayer = MediaPlayer.create(context, R.raw.typing)
        erasePlayer = MediaPlayer.create(context, R.raw.erase)
        downloadPlayer = MediaPlayer.create(context, R.raw.download)
        langPlayer = MediaPlayer.create(context, R.raw.lang)
    }

    fun playTyping() {
        playSound(typingPlayer)
    }

    fun playErase() {
        playSound(erasePlayer)
    }

    fun playDownload() {
        playSound(downloadPlayer)
    }

    fun playLang() {
        playSound(langPlayer)
    }

    private fun playSound(player: MediaPlayer?) {
        player?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            } else {
                it.start()
            }
        }
    }

    fun release() {
        typingPlayer?.release()
        erasePlayer?.release()
        downloadPlayer?.release()
        langPlayer?.release()
        
        typingPlayer = null
        erasePlayer = null
        downloadPlayer = null
        langPlayer = null
    }
}