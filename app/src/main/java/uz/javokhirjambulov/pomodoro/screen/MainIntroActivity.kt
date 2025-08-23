package uz.javokhirjambulov.pomodoro.screen

import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import uz.javokhirjambulov.pomodoro.R

class MainIntroActivity: IntroActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_avoid_distractions)
                .description(R.string.intro_avoid_distractions_description)
                .image(R.drawable.intro1)
                .background(R.color.grey50)
                .backgroundDark(R.color.grey50)
                .layout(R.layout.activity_main_intro)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_clear_mind)
                .description(R.string.intro_clear_mind_description)
                .image(R.drawable.intro2)
                .background(R.color.grey50)
                .backgroundDark(R.color.grey50)
                .layout(R.layout.activity_main_intro)
                .build()
        )
        addSlide(
            SimpleSlide.Builder()
                .title(R.string.intro_get_started)
                .description(R.string.intro_get_started_description)
                .image(R.drawable.intro3)
                .background(R.color.grey50)
                .backgroundDark(R.color.grey50)
                .layout(R.layout.activity_main_intro)
                .build()
        )
        val root = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}