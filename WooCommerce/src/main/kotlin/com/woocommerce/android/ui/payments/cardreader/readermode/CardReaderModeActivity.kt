package com.woocommerce.android.ui.payments.cardreader.readermode

import android.Manifest
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooPermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CardReaderModeActivity : AppCompatActivity() {

    private val viewModel: CardReaderModeViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(
            granted = granted,
            shouldShowRationale = WooPermissionUtils.shouldShowFineLocationPermissionRationale(this),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        setContent {
            WooThemeWithBackground {
                CardReaderModeScreen(viewModel = viewModel)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        CardReaderModeEvent.Exit -> finish()
                        CardReaderModeEvent.RequestLocationPermission ->
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        CardReaderModeEvent.OpenAppSettings ->
                            WooPermissionUtils.showAppSettings(this@CardReaderModeActivity, openInNewStack = false)
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            if (WooPermissionUtils.hasFineLocationPermission(this)) {
                viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)
            } else {
                viewModel.onLocationPermissionMissing()
            }
        }
    }
}
