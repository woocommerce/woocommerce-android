package com.woocommerce.android.ui.woopos.home.items.coupons.creation

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.adjustActivityTransition
import com.woocommerce.android.ui.coupons.create.CouponTypePickerFragmentArgs
import com.woocommerce.android.ui.woopos.util.ext.isGestureNavigation
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosCouponCreationActivity : AppCompatActivity(R.layout.activity_woo_pos_coupon_creation) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTopAndBottomInsets()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.woopos_coupon_creation_nav_host_fragment
        ) as NavHostFragment

        setupNavGraph(navHostFragment)
        observeResult(navHostFragment)
    }

    private fun setupTopAndBottomInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rootView = findViewById<View>(R.id.snack_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            insets.toWindowInsets()?.let { windowInsets ->
                val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets, view)
                val isGestureNavigation = insetsCompat.isGestureNavigation(this)

                val topPadding = insetsCompat.getInsets(WindowInsetsCompat.Type.statusBars()).top
                val bottomPadding = if (isGestureNavigation) {
                    0
                } else {
                    insetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                }
                view.updatePadding(
                    top = topPadding,
                    bottom = bottomPadding
                )
            }

            insets
        }
    }

    override fun finish() {
        super.finish()
        adjustActivityTransition(
            overrideTransitionOpen = true,
            enterAnim = android.R.anim.fade_in,
            exitAnim = android.R.anim.fade_out,
        )
    }

    private fun observeResult(navHostFragment: NavHostFragment) {
        navHostFragment.childFragmentManager.setFragmentResultListener(
            WOO_POS_COUPON_CREATION_REQUEST_KEY,
            this
        ) { requestKey, bundle ->
            when (requestKey) {
                WOO_POS_COUPON_CREATION_REQUEST_KEY -> {
                    val resultIntent = Intent()
                    if (bundle.containsKey(WOO_POS_COUPON_CREATION_NEW_COUPON_ID)) {
                        resultIntent.putExtra(
                            WOO_POS_COUPON_CREATION_NEW_COUPON_ID,
                            bundle.getLong(WOO_POS_COUPON_CREATION_NEW_COUPON_ID)
                        )
                        setResult(RESULT_OK, resultIntent)
                    } else {
                        setResult(RESULT_CANCELED)
                    }
                    finish()
                }

                else -> logResultListenerError(requestKey)
            }
        }
    }

    private fun setupNavGraph(navHostFragment: NavHostFragment) {
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph_coupons).apply {
            setStartDestination(R.id.couponTypePickerFragment)
        }
        navController.setGraph(graph, CouponTypePickerFragmentArgs(isPOSMode = true).toBundle())
    }
    private fun logResultListenerError(requestKey: String) {
        val errorMessage = "Unknown request key: $requestKey"
        WooLog.e(WooLog.T.POS, "Error in WooPosCouponCreationActivity - $errorMessage")
        error(errorMessage)
    }

    companion object {
        const val WOO_POS_COUPON_CREATION_REQUEST_KEY = "woo_pos_coupon_creation_request"
        const val WOO_POS_COUPON_CREATION_NEW_COUPON_ID = "woo_pos_coupon_new_coupon_id"

        fun buildIntentForCouponCreation(context: Context) =
            Intent(context, WooPosCouponCreationActivity::class.java)
    }
}
