package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.Lazy
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

/**
 * All product related fragments should extend this class to provide a consistent method
 * of displaying snackbars and handling navigation
 */
abstract class BaseProductFragment : BaseFragment, BackPressListener {
    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    protected val viewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products) {
        viewModelFactory.get()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // if this is the initial creation of this fragment, tell the viewModel to make a copy of the product
        // as it exists now so we can easily discard changes are determine if any changes were made inside
        // this fragment
        if (savedInstanceState == null) {
            viewModel.updateProductBeforeEnteringFragment()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> requireActivity().onBackPressed()
                is ShowDialog -> WooDialog.showDialog(
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
                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }
}
