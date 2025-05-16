package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartfreezer.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTitle.text = getString(R.string.about_title)
        binding.tvDescription.text = getString(R.string.about_description)
        binding.tvFirebase.text = getString(R.string.about_firebase)
        binding.tvSpoonacular.text = getString(R.string.about_spoonacular)
        binding.tvVersion.text = getString(R.string.about_version)
        binding.tvContact.text = getString(R.string.about_contact)

        binding.buttonFeedBack.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}