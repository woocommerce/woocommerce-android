package com.woocommerce.android.ui.woopos.home.items.coupons.creation

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.adjustActivityTransition
import com.woocommerce.android.extensions.getColorCompat
import com.woocommerce.android.ui.coupons.create.CouponTypePickerFragmentArgs
import com.woocommerce.android.ui.woopos.util.ext.lockWooPosOrientation
import com.woocommerce.android.ui.woopos.util.ext.setupWooPosTopAndBottomInsets
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosCouponCreationActivity : AppCompatActivity(R.layout.activity_woo_pos_coupon_creation) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFixForStatusBarColor()
        setupWooPosTopAndBottomInsets(R.id.snack_root)
        lockWooPosOrientation()

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.woopos_coupon_creation_nav_host_fragment
        ) as NavHostFragment

        setupNavGraph(navHostFragment)
        observeResult(navHostFragment)
    }

    private fun applyFixForStatusBarColor() {
        val isDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val statusBarColor = getColorCompat(R.color.color_toolbar)
        val statusBarStyle = if (isDark) {
            SystemBarStyle.dark(statusBarColor)
        } else {
            SystemBarStyle.light(statusBarColor, statusBarColor)
        }
        enableEdgeToEdge(statusBarStyle = statusBarStyle)
    }

    override fun finish() {
        super.finish()
        adjustActivityTransition(
            overrideTransitionOpen = false,
            enterAnim = R.anim.woopos_slide_in_left,
            exitAnim = R.anim.woopos_slide_out_right
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
        val graph = navController.navInflater.inflate(R.navigation.nav_graph_main)

        graph.setStartDestination(R.id.nav_graph_coupons)

        val couponsGraph = graph.findNode(R.id.nav_graph_coupons) as NavGraph
        couponsGraph.setStartDestination(R.id.couponTypePickerFragment)

        val args = CouponTypePickerFragmentArgs(isPOSMode = true).toBundle()
        navController.setGraph(graph, args)
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
