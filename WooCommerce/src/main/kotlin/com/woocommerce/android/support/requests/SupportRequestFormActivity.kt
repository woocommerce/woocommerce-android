package com.woocommerce.android.support.requests

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
            setupActionBar()
            observeViewEvents(this)
            // Disabled to avoid triggering a support request while testing, it will be removed later
            submitRequestButton.isEnabled = false
        }
    }

    private fun ActivitySupportRequestFormBinding.setupActionBar() {
        setSupportActionBar(toolbar.toolbar as Toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewEvents(binding: ActivitySupportRequestFormBinding) {
        binding.submitRequestButton.setOnClickListener {
            viewModel.onSubmitRequestButtonClicked(
                this,
                TicketType.General,
                "This is a Test",
                "Please Ignore"
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
