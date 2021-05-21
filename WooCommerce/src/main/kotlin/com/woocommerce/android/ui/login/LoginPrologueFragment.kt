package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueBinding
import com.woocommerce.android.ui.login.LoginPrologueViewPagerIndicator.OnIndicatorClickedListener
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class LoginPrologueFragment : androidx.fragment.app.Fragment(R.layout.fragment_login_prologue) {
    companion object {
        const val TAG = "login-prologue-fragment"

        fun newInstance(): LoginPrologueFragment {
            return LoginPrologueFragment()
        }
    }

    interface PrologueFinishedListener {
        fun onPrimaryButtonClicked()
        fun onSecondaryButtonClicked()
    }

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    private var prologueFinishedListener: PrologueFinishedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLoginPrologueBinding.bind(view)

        binding.buttonLoginStore.setOnClickListener {
            // Login with site address
            prologueFinishedListener?.onPrimaryButtonClicked()
        }

        binding.buttonLoginWpcom.setOnClickListener {
            // Login with WordPress.com account
            prologueFinishedListener?.onSecondaryButtonClicked()
        }

        binding.viewPager.initViewPager(childFragmentManager)
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                binding.viewPagerIndicator.setSelectedIndicator(position)
            }
        })

        binding.viewPagerIndicator.setListener(object : OnIndicatorClickedListener {
            override fun onIndicatorClicked(index: Int) {
                binding.viewPager.currentItem = index
            }
        })

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (activity is PrologueFinishedListener) {
            prologueFinishedListener = activity as PrologueFinishedListener
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, Step.PROLOGUE)
    }

    override fun onDetach() {
        super.onDetach()
        prologueFinishedListener = null
    }
}
