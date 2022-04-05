package com.woocommerce.android.ui.coupons.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCouponDetailsBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CouponDetailsEvent.CopyCodeEvent
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.copyToClipboard
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import java.lang.IllegalStateException

@AndroidEntryPoint
class CouponDetailsFragment : BaseFragment(R.layout.fragment_coupon_details) {
    private var _binding: FragmentCouponDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CouponDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCouponDetailsBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.couponsComposeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CouponDetailsScreen(viewModel) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CopyCodeEvent -> copyCodeToClipboard(event.couponCode)
            }
        }
    }

    private fun copyCodeToClipboard(couponCode: String) {
        try {
            context?.copyToClipboard(getString(R.string.coupon_details_copy_clipboard_label), couponCode)
            ToastUtils.showToast(context, R.string.coupon_details_copy_success)
        } catch (e: IllegalStateException) {
            WooLog.e(WooLog.T.UTILS, e)
            ToastUtils.showToast(context, R.string.coupon_details_copy_error)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
