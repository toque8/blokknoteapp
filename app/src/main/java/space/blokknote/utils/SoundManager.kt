package space.blokknote.utils

import android.content.Context
import android.media.MediaPlayer
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundManager @Inject constructor(
    private val context: Context
) {
    private val playerPool = ConcurrentLinkedQueue<MediaPlayer>()

    fun playTyping() {
        playSound(R.raw.typing)
    }

    fun playErase() {
        playSound(R.raw.erase)
    }

    fun playDownload() {
        playSound(R.raw.download)
    }

    fun playLang() {
        playSound(R.raw.lang)
    }

    private fun playSound(resId: Int) {
        try {
            val player = MediaPlayer.create(context, resId)
            
            if (player != null) {
                playerPool.add(player)
                
                player.setOnCompletionListener { mp ->
                    mp.release()
                    playerPool.remove(mp)
                }
                
                player.start()
            }
        } catch (e: Exception) {
        }
    }

    fun release() {
        while (playerPool.isNotEmpty()) {
            val player = playerPool.poll()
            try {
                if (player?.isPlaying == true) {
                    player.stop()
                }
                player?.release()
            } catch (e: Exception) {
            }
        }
        playerPool.clear()
    }
}
