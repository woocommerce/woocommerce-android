package com.woocommerce.android.ui.coupons.details

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCouponDetailsBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.CopyCodeEvent
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.ShareCodeEvent
import com.woocommerce.android.ui.coupons.details.CouponDetailsViewModel.ShowEditCoupon
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.copyToClipboard
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class CouponDetailsFragment : BaseFragment(R.layout.fragment_coupon_details) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: CouponDetailsViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentCouponDetailsBinding.inflate(inflater, container, false)

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is CopyCodeEvent -> copyCodeToClipboard(event.couponCode)
                is ShareCodeEvent -> shareCode(event.shareCodeMessage)
                is ShowEditCoupon -> findNavController().navigate(
                    CouponDetailsFragmentDirections.actionCouponDetailsFragmentToEditCouponFragment(event.couponId)
                )
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().navigateUp()
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

    private fun shareCode(shareCodeMessage: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareCodeMessage)
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share)))
        } catch (e: android.content.ActivityNotFoundException) {
            WooLog.e(WooLog.T.UTILS, e)
            ToastUtils.showToast(context, R.string.coupon_details_share_coupon_error)
        }
    }
}
