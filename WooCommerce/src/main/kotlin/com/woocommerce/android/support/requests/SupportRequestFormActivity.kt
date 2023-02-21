package com.woocommerce.android.support.requests

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivitySupportRequestFormBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SupportRequestFormActivity : AppCompatActivity() {
    private val viewModel: SupportRequestFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivitySupportRequestFormBinding.inflate(layoutInflater).apply {
            setContentView(root)

        }
    }

    private fun ActivitySupportRequestFormBinding.observeUIChanges() {
        submitRequestButton.setOnClickListener { viewModel.onSubmitRequestButtonClicked() }
    }
}
