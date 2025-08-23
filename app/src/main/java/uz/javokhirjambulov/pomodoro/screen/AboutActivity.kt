package uz.javokhirjambulov.pomodoro.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import uz.javokhirjambulov.pomodoro.R
import uz.javokhirjambulov.pomodoro.commons.Constants
import uz.javokhirjambulov.pomodoro.databinding.ActivityAboutBinding
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutActivity : AppCompatActivity(){
    private lateinit var binding: ActivityAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_about)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.viewSourceCode.setOnClickListener{
            val openGithub = Intent(Intent.ACTION_VIEW, Constants.sourceCodeURL.toUri())
            startActivity(openGithub)
        }
        binding.sendFeedback.setOnClickListener {
            val emailIntent = Intent(
                Intent.ACTION_SENDTO,
                String.format(Constants.feedbackURL, getString(R.string.app_name)).toUri()
            )

            startActivity(emailIntent)
        }
        binding.appIntro.setOnClickListener {
            // show app intro
            val i = Intent(this, MainIntroActivity::class.java)
            startActivity(i)
        }
    }
}