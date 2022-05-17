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
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.edit.EditCouponViewModel.NavigateToCouponRestrictionsEvent
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlin.properties.Delegates.observable

@AndroidEntryPoint
class EditCouponFragment : BaseFragment() {
    private val viewModel: EditCouponViewModel by viewModels()

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
                is NavigateToCouponRestrictionsEvent -> {
                    findNavController().navigateSafely(
                        EditCouponFragmentDirections.actionEditCouponFragmentToCouponRestrictionsFragment(
                            event.restrictions
                        )
                    )
                }
            }
        }
    }

    private fun handleResults() {
        handleResult<String>(SimpleTextEditorFragment.SIMPLE_TEXT_EDITOR_RESULT) {
            viewModel.onDescriptionChanged(it)
        }
    }

    override fun getFragmentTitle() = screenTitle
}
