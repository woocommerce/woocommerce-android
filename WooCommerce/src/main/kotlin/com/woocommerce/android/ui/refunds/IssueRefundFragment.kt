package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.UIMessageResolver
import javax.inject.Inject
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType.AMOUNT
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType.ITEMS
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_refund_by_amount.*
import org.wordpress.android.util.ActivityUtils

class IssueRefundFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) {
        viewModelFactory.get()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        return inflater.inflate(R.layout.fragment_issue_refund, container, false)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager
                .beginTransaction()
                .replace(R.id.issueRefund_frame, RefundByItemsFragment())
                .commit()

        // TODO: Temporary; it will be used again in a future release - do not remove
//        initializeViews(viewModel)
        setupObservers(viewModel)
    }

    // TODO: Temporary; it will be used again in a future release - do not remove
//    private fun initializeViews(viewModel: IssueRefundViewModel) {
//        issueRefund_viewPager.adapter = RefundPageAdapter(childFragmentManager)
//        issueRefund_viewPager.addOnPageChangeListener(object : OnPageChangeListener {
//            override fun onPageScrollStateChanged(state: Int) {}
//            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
//
//            override fun onPageSelected(position: Int) {
//                viewModel.onRefundTabChanged(RefundType.values()[position])
//            }
//        })
//        issueRefund_tabLayout.setupWithViewPager(issueRefund_viewPager)
//    }

    private fun setupObservers(viewModel: IssueRefundViewModel) {
        viewModel.commonStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.screenTitle?.takeIfNotEqualTo(old?.screenTitle) { requireActivity().title = it }

            if (new.refundType == AMOUNT) {
                issueRefund_refundAmount.requestFocus()
                ActivityUtils.showKeyboard(issueRefund_refundAmount)
            } else {
                ActivityUtils.hideKeyboard(requireActivity())
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowRefundSummary -> {
                    val action = IssueRefundFragmentDirections.actionIssueRefundFragmentToRefundSummaryFragment()
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        })
    }

    // TODO: Temporarily unused; it will be used again in a future release - do not remove
    @Suppress("unused")
    @SuppressLint("WrongConstant")
    private class RefundPageAdapter(
        fragmentManager: FragmentManager
    ) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            return when (RefundType.values()[position]) {
                ITEMS -> RefundByItemsFragment()
                AMOUNT -> RefundByAmountFragment()
            }
        }

        override fun getCount(): Int {
            return RefundType.values().size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return RefundType.values()[position].name
        }
    }
}
