package com.woocommerce.android.ui.prefs

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import com.automattic.about.model.AboutConfigProvider
import com.woocommerce.android.R
import com.woocommerce.android.extensions.edgeToEdgeHandlingForNavigationAndStatusBar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UnifiedAboutScreenActivity : AppCompatActivity(), AboutConfigProvider {
    @Inject
    lateinit var configBuilder: AboutConfigBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.unified_about_screen_activity)

        val fragmentContainer = findViewById<FragmentContainerView>(
            R.id.about_fragment_container
        )
        fragmentContainer.edgeToEdgeHandlingForNavigationAndStatusBar()
    }

    override fun getAboutConfig() = configBuilder.createAboutConfig(this)
}
