package com.woocommerce.android.ui.coupons.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorFragment
import com.woocommerce.android.ui.products.selector.ProductSelectorFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUiStringSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates.observable

@AndroidEntryPoint
class EditCouponFragment : BaseFragment() {
    private val viewModel: EditCouponViewModel by viewModels()

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp
        )

    private var screenTitle: String by observable("") { _, oldValue, newValue ->
        if (oldValue != newValue) {
            updateActivityTitle()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    EditCouponScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        handleResults()
    }

    private fun setupObservers() {
        viewModel.viewState.observe(viewLifecycleOwner) {
            screenTitle = getString(R.string.coupon_edit_screen_title, it.localizedType)
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is EditCouponNavigationTarget -> EditCouponNavigator.navigate(this, event)
                is Exit -> findNavController().navigateUp()
                is ShowUiStringSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }

    private fun handleResults() {
        handleResult<String>(SimpleTextEditorFragment.SIMPLE_TEXT_EDITOR_RESULT) {
            viewModel.onDescriptionChanged(it)
        }

        handleResult<Coupon.CouponRestrictions>(CouponRestrictionsFragment.RESTRICTIONS_RESULT) {
            viewModel.onRestrictionsUpdated(it)
        }

        handleResult<Set<Long>>(ProductSelectorFragment.PRODUCT_SELECTOR_RESULT) {
            viewModel.onSelectedProductsUpdated(it)
        }

        handleResult<Set<Long>>(ProductCategorySelectorFragment.PRODUCT_CATEGORY_SELECTOR_RESULT) {
            viewModel.onIncludedCategoriesChanged(it)
        }
    }

    override fun getFragmentTitle() = screenTitle
}
