package com.woocommerce.android.ui.products.inventory

import android.Manifest
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ProductInfo
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ViewState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanToUpdateInventoryScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val registry = RecordingActivityResultRegistry()
    private var productDetailsOpened = false

    @Test
    fun given_external_scanner_input_when_inventory_is_shown_then_camera_permission_is_not_requested() {
        // WHEN
        givenScreen(cameraScanningEnabled = false, showProduct = true)

        // THEN
        composeTestRule.onNodeWithText(PRODUCT_NAME).assertIsDisplayed()
        composeTestRule.runOnIdle {
            assertThat(registry.requestedPermissions).isEmpty()
        }
        val detailsLabel = InstrumentationRegistry.getInstrumentation()
            .targetContext.getString(R.string.scan_to_update_inventory_product_details_button)
        composeTestRule.onNodeWithText(detailsLabel).performScrollTo().performClick()
        composeTestRule.runOnIdle {
            assertThat(productDetailsOpened).isTrue()
        }
    }

    @Test
    fun given_camera_entry_when_scanner_opens_then_camera_permission_is_requested() {
        // WHEN
        givenScreen(cameraScanningEnabled = true, showProduct = false)

        // THEN
        composeTestRule.runOnIdle {
            assertThat(registry.requestedPermissions).containsExactly(Manifest.permission.CAMERA)
        }
    }

    private fun givenScreen(cameraScanningEnabled: Boolean, showProduct: Boolean) {
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        val viewState = mutableStateOf<ViewState>(
            if (showProduct) {
                ViewState.QuickInventoryBottomSheetVisible(
                    product = ProductInfo(
                        id = 1L,
                        name = PRODUCT_NAME,
                        imageUrl = "",
                        sku = "EXT-123",
                        quantity = 1,
                        isStockManaged = false,
                    )
                )
            } else {
                ViewState.QuickInventoryBottomSheetHidden
            }
        )
        val permissionState = mutableStateOf(BarcodeScanningViewModel.PermissionState.Unknown)
        val isHardwareScannerInfoVisible = mutableStateOf(false)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                ScanToUpdateInventoryScreen(
                    cameraScanningEnabled = cameraScanningEnabled,
                    onNewFrame = {},
                    onBindingException = {},
                    permissionState = permissionState,
                    onCameraPermissionResult = {},
                    viewState = viewState,
                    onBottomSheetDismissed = {},
                    onIncrementQuantityClicked = {},
                    onUpdateQuantityClicked = {},
                    onViewProductDetailsClicked = { productDetailsOpened = true },
                    onManualQuantityEntered = {},
                    onManageStockClicked = {},
                    isHardwareScannerInfoVisible = isHardwareScannerInfoVisible,
                    onHardwareScannerInfoClicked = {},
                    onHardwareScannerTestScanSucceeded = {},
                    onHardwareScannerTestScanFailed = {},
                    onHardwareScannerInfoDismissed = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private class RecordingActivityResultRegistry : ActivityResultRegistry() {
        val requestedPermissions = mutableListOf<String>()

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            requestedPermissions.add(input as String)
            dispatchResult(requestCode, false)
        }
    }

    private companion object {
        const val PRODUCT_NAME = "Scanner product"
    }
}
