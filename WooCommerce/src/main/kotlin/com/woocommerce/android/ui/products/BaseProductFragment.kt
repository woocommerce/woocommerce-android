package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

/**
 * All product related fragments should extend this class to provide a consistent method
 * of displaying snackbars and handling navigation
 */
abstract class BaseProductFragment : BaseFragment, BackPressListener {
    @Inject lateinit var navigator: ProductNavigator

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    protected val viewModel: ProductDetailViewModel by hiltNavGraphViewModels(R.id.nav_graph_products)

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowDialog -> showBackResolutionDialog(event)
                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        }
    }

    private fun showBackResolutionDialog(event: ShowDialog) {
        if (!tracksPendingBackResolution) {
            WooDialog.showDialog(
                requireActivity(),
                event.positiveBtnAction,
                event.negativeBtnAction,
                event.neutralBtnAction,
                titleId = event.titleId,
                messageId = event.messageId,
                positiveButtonId = event.positiveButtonId,
                negativeButtonId = event.negativeButtonId,
                neutralButtonId = event.neutralButtonId
            )
            return
        }

        var resolutionContinues = false
        fun continuingAction(action: DialogInterface.OnClickListener?) = action?.let {
            DialogInterface.OnClickListener { dialog, which ->
                resolutionContinues = true
                it.onClick(dialog, which)
            }
        }
        val cancellingAction = event.negativeButtonId?.let {
            DialogInterface.OnClickListener { dialog, which ->
                clearPendingBackResolution()
                event.negativeBtnAction?.onClick(dialog, which)
            }
        }

        WooDialog.showDialog(
            activity = requireActivity(),
            posBtnAction = continuingAction(event.positiveBtnAction),
            negBtnAction = cancellingAction,
            neutBtAction = continuingAction(event.neutralBtnAction),
            titleId = event.titleId,
            messageId = event.messageId,
            positiveButtonId = event.positiveButtonId,
            negativeButtonId = event.negativeButtonId,
            neutralButtonId = event.neutralButtonId,
            cancellable = event.cancelable,
            onDismiss = {
                if (!resolutionContinues) {
                    clearPendingBackResolution()
                }
                event.onDismiss?.invoke()
            }
        )
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }
}
