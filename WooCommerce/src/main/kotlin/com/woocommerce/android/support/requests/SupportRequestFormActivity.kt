package com.woocommerce.android.support.requests

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.databinding.ActivitySupportRequestFormBinding
import com.woocommerce.android.support.TicketType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SupportRequestFormActivity : AppCompatActivity() {
    private val viewModel: SupportRequestFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivitySupportRequestFormBinding.inflate(layoutInflater).apply {
            setContentView(root)
            observeUIChanges(this)
        }
    }

    private fun observeUIChanges(binding: ActivitySupportRequestFormBinding) {
        binding.submitRequestButton.setOnClickListener {
            viewModel.onSubmitRequestButtonClicked(
                this,
                TicketType.General,
                "This is a Test",
                "Please Ignore"
            )
        }
    }
}
