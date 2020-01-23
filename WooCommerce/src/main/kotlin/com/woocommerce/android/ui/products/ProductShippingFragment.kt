package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.products.ProductShippingClassDialog.ShippingClassDialogListener
import com.woocommerce.android.ui.products.ProductShippingViewModel.ViewState
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import kotlinx.android.synthetic.main.fragment_product_shipping.*
import org.wordpress.android.fluxc.model.WCProductShippingClassModel
import javax.inject.Inject

/**
 * Fragment which enables updating product shipping data.
 */
class ProductShippingFragment : BaseFragment(), ShippingClassDialogListener {
    companion object {
        private const val KEY_IS_SHIPPING_CLASS_DLG_SHOWING = "shipping_class_dlg_showing"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val navArgs: ProductShippingFragmentArgs by navArgs()
    private var isShippingClassDlgShowing: Boolean = false
    private val viewModel: ProductShippingViewModel by viewModels { viewModelFactory }
    private var shippingClassDialog: ProductShippingClassDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_shipping, container, false)
    }

    override fun onPause() {
        shippingClassDialog?.dismiss()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()

        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_SHIPPING_CLASS_DLG_SHOWING)) {
                showShippingClassDialog()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_SHIPPING_CLASS_DLG_SHOWING, isShippingClassDlgShowing)
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_settings)

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: ProductShippingViewModel) {
        viewModel.viewStateLiveData.observe(viewLifecycleOwner) { old, new ->
            new.product?.takeIfNotEqualTo(old?.product) { showProduct(new) }
        }

        viewModel.productShippingClasses.observe(viewLifecycleOwner, Observer { shippingClasses ->
            shippingClassDialog?.setShippingClasses(shippingClasses)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                        requireActivity(),
                        event.positiveBtnAction,
                        event.negativeBtnAction
                )
            }
        })
    }

    /**
     * Shows the passed weight or dimension value in the passed view and sets the hint so it
     * includes the weight or dimension unit, ex: "Width (in)"
     */
    private fun showValue(view: WCMaterialOutlinedEditTextView, @StringRes hintRes: Int, value: Float?, unit: String?) {
        view.setText(value?.toString() ?: "")
        view.setHint(getString(hintRes) + unit?.let {
            " ($it)"
        })
    }

    private fun showProduct(productData: ViewState) {
        if (!isAdded) return

        showValue(product_weight, R.string.product_weight, productData.product?.weight, viewModel.weightUnit)
        showValue(product_length, R.string.product_length, productData.product?.length, viewModel.dimensionUnit)
        showValue(product_height, R.string.product_height, productData.product?.height, viewModel.dimensionUnit)
        showValue(product_width, R.string.product_width, productData.product?.width, viewModel.dimensionUnit)

        product_shipping_class_spinner.setText(productData.product?.shippingClass ?: "")
        product_shipping_class_spinner.setClickListener {
            showShippingClassDialog()
        }
    }

    private fun showShippingClassDialog() {
        isShippingClassDlgShowing = true
        shippingClassDialog = ProductShippingClassDialog.newInstance(this@ProductShippingFragment).also { dialog ->
            dialog.show(parentFragmentManager, ProductShippingClassDialog.TAG)
        }
    }

    /**
     * Shipping class dialog is requesting data
     */
    override fun onRequestShippingClasses(loadMore: Boolean) {
        viewModel.loadProductShippingClasses(loadMore)
    }

    /**
     * User made a selection from the shipping class dialog
     */
    override fun onShippingClassClicked(shippingClass: WCProductShippingClassModel) {
        product_shipping_class_spinner.setText(shippingClass.name)
        shippingClassDialog?.dismiss()
        shippingClassDialog = null
        isShippingClassDlgShowing = false
        // TODO: update product draft in another PR
    }

    /**
     * Shipping class dialog was cancelled without making a selection
     */
    override fun onShippingClassDialogCancelled() {
        isShippingClassDlgShowing = false
    }
}
