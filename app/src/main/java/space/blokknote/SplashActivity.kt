package space.blokknote

import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_splash)
        
        val splashIcon = findViewById<ImageView>(R.id.splash_icon)
        
        val isDarkTheme = (resources.configuration.uiMode and 0x30) == 0x20
        
        val tintColor = if (isDarkTheme) {
            ContextCompat.getColor(this, R.color.white) // Светлый карандаш в темной теме
        } else {
            ContextCompat.getColor(this, R.color.primary) // Темный карандаш в светлой теме
        }
        
        splashIcon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_forever)
        splashIcon.startAnimation(rotateAnimation)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 2500)
    }
}
