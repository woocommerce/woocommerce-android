package com.woocommerce.android.ui.orders.creation.product.details

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditProductDetailsBinding
import com.woocommerce.android.databinding.ProductItemViewBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.product.details.OrderCreateEditProductDetailsViewModel.ProductDetailsEditResult
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreateEditProductDetailsFragment :
    BaseFragment(R.layout.fragment_order_create_edit_product_details) {
    private val viewModel: OrderCreateEditProductDetailsViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentOrderCreateEditProductDetailsBinding.bind(view)

        with(binding) {
            removeProductButton.setOnClickListener {
                viewModel.onProductRemoved()
            }
            toolbar.setNavigationOnClickListener {
                viewModel.onCloseClicked()
            }
        }

        viewModel.viewState.observe(viewLifecycleOwner) { state ->
            binding.productItemView.binding.renderProductItemView(state)
            binding.renderDiscountSection(state)
            binding.renderAddDiscountButton(state)
        }

        viewModel.event.observe(viewLifecycleOwner, ::handleNavEvent)
    }

    private fun handleNavEvent(event: MultiLiveEvent.Event) {
        when (event) {
            is OrderCreateEditProductDetailsViewModel.NavigationTarget.DiscountCreate -> {}
            is OrderCreateEditProductDetailsViewModel.NavigationTarget.DiscountEdit -> {}
            is MultiLiveEvent.Event.ExitWithResult<*> -> {
                navigateBackWithResult(KEY_PRODUCT_DETAILS_EDIT_RESULT, event.data as ProductDetailsEditResult)
            }
            is MultiLiveEvent.Event.Exit -> {
                findNavController().navigateUp()
            }
        }
    }

    private fun ProductItemViewBinding.renderProductItemView(
        state: OrderCreateEditProductDetailsViewModel.ViewState
    ) {
        divider.isVisible = false
        renderProductImage(state.productDetailsState.imageUrl)
        productName.text = state.productDetailsState.title
        productSku.text = state.productDetailsState.skuSubtitle
        productStockAndStatus.text = state.productDetailsState.stockPriceSubtitle
    }

    private fun ProductItemViewBinding.renderProductImage(imageUrl: String) {
        productImageSelected.isVisible = false
        val imageSize = resources.getDimensionPixelSize(R.dimen.image_minor_100)
        val imageCornerRadius = resources.getDimensionPixelSize(R.dimen.corner_radius_image)
        val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
        GlideApp.with(requireContext())
            .load(photonUrl)
            .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
            .placeholder(R.drawable.ic_product)
            .into(productImage)
    }

    private fun FragmentOrderCreateEditProductDetailsBinding.renderAddDiscountButton(
        state: OrderCreateEditProductDetailsViewModel.ViewState
    ) {
        addDiscountButton.isVisible =
            state.addDiscountButtonVisible && FeatureFlag.ORDER_CREATION_PRODUCT_DISCOUNTS.isEnabled()
    }

    private fun FragmentOrderCreateEditProductDetailsBinding.renderDiscountSection(
        state: OrderCreateEditProductDetailsViewModel.ViewState
    ) = with(discountSection) {
        isVisible =
            state.discountSectionState.isVisible && FeatureFlag.ORDER_CREATION_PRODUCT_DISCOUNTS.isEnabled()
        // To be continued
    }

    companion object {
        const val KEY_PRODUCT_DETAILS_EDIT_RESULT = "key_product_details_edit_result"
    }
}
