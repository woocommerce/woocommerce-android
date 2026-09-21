package com.woocommerce.android.ui.woopos.cardreader

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingFragmentArgs
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.woopos.util.ext.isGestureNavigation
import com.woocommerce.android.ui.woopos.util.ext.lockWooPosOrientation
import com.woocommerce.android.util.parcelable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosCardReaderOnboardingActivity : AppCompatActivity(R.layout.activity_woo_pos_card_reader_onboarding) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupTopAndBottomInsets()
        lockWooPosOrientation()

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.woopos_card_reader_onboarding_nav_host_fragment
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
            WOO_POS_CARD_ONBOARDING_REQUEST_KEY,
            this
        ) { _, _ ->
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun setupNavGraph(navHostFragment: NavHostFragment) {
        val navController = navHostFragment.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph_payment_flow)

        val onboardingState = intent.parcelable<CardReaderOnboardingState>(KEY_ONBOARDING_STATE)
            ?: run {
                finish()
                return
            }

        graph.setStartDestination(R.id.cardReaderOnboardingFragment)
        navController.setGraph(
            graph,
            CardReaderOnboardingFragmentArgs(
                cardReaderOnboardingParam = CardReaderOnboardingParams.Failed(
                    cardReaderFlowParam = CardReaderFlowParam.WooPosConnection,
                    onboardingState = onboardingState
                ),
                cardReaderType = CardReaderType.EXTERNAL
            ).toBundle()
        )
    }

    companion object {
        const val WOO_POS_CARD_ONBOARDING_REQUEST_KEY = "woo_pos_card_onboarding_request"
        private const val KEY_ONBOARDING_STATE = "key_onboarding_state"

        fun buildIntent(context: Context, onboardingState: CardReaderOnboardingState): Intent {
            return Intent(context, WooPosCardReaderOnboardingActivity::class.java).apply {
                putExtra(KEY_ONBOARDING_STATE, onboardingState)
            }
        }
    }
}
