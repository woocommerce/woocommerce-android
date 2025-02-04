package com.woocommerce.android.ui.prefs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentContainerView
import com.automattic.about.model.AboutConfigProvider
import com.woocommerce.android.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UnifiedAboutScreenActivity : AppCompatActivity(), AboutConfigProvider {
    @Inject lateinit var configBuilder: AboutConfigBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unified_about_screen_activity)

        applyTopInset()
    }

    private fun applyTopInset() {
        val fragmentContainer = findViewById<FragmentContainerView>(
            R.id.about_fragment_container
        )
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun getAboutConfig() = configBuilder.createAboutConfig(this)
}
