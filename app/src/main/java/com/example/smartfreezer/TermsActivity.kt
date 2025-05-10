package com.example.smartfreezer

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartfreezer.databinding.ActivityTermsBinding

class TermsActivity : BaseActivity() {
    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvTitle.text = getString(R.string.terms_title)
        binding.tvTerms.text = getString(R.string.terms_text)
        binding.tvPrivacyTitle.text = getString(R.string.privacy_title)
        binding.tvPrivacy.text = getString(R.string.privacy_text)
        binding.tvThirdPartyTitle.text = getString(R.string.third_party_title)
        binding.tvThirdParty.text = getString(R.string.third_party_text)

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}