package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isTablet
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class UpdateProductStockStatusFragment : DialogFragment() {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: UpdateProductStockStatusViewModel by viewModels()

    companion object {
        const val UPDATE_STOCK_STATUS_EXIT_STATE_KEY = "update_stock_status_exit_state_key"
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTablet()) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
        } else {
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            UpdateProductStockStatusScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            dialog?.window?.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun isTabletLandscape(): Boolean {
        val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)
        val isLandscape = DisplayUtils.isLandscape(context)

        return isTablet && isLandscape
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is MultiLiveEvent.Event.ExitWithResult<*> -> {
                    navigateBackWithResult(
                        UPDATE_STOCK_STATUS_EXIT_STATE_KEY,
                        event.data
                    )
                }

                is MultiLiveEvent.Event.ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }

                else -> event.isHandled = false
            }
        }
    }
}
