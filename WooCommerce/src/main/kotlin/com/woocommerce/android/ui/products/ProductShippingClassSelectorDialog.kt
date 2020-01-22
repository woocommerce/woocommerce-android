package com.woocommerce.android.ui.products

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.OnShippingClassClickListener
import kotlinx.android.synthetic.main.dialog_product_shipping_class_list.*
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

/**
 * Dialog displays a list of product shipping classes
 *
 * This fragment should be instantiated using the [ProductShippingClassSelectorDialog.newInstance] method.
 * Calling classes can obtain the results of selection through the [onActivityResult]
 * via [ProductShippingClassSelectorDialog.getTargetFragment].
 *
 * The [resultCode] passed to this fragment is used to classify the product shipping class
 */
class ProductShippingClassSelectorDialog : DialogFragment(), OnShippingClassClickListener {
    companion object {
        const val TAG: String = "ProductShippingSelectorDialog"

        fun newInstance(
            listener: Fragment,
            resultCode: Int,
            dialogTitle: String,
            selectedListItem: String?
        ): ProductShippingClassSelectorDialog {
            val fragment = ProductShippingClassSelectorDialog()
            fragment.setTargetFragment(listener, RequestCodes.PRODUCT_SHIPPING_CLASS)
            fragment.retainInstance = true
            fragment.resultCode = resultCode
            fragment.dialogTitle = dialogTitle
            fragment.selectedListItem = selectedListItem
            return fragment
        }
    }

    interface ProductShippingClassSelectorDialogListener {
        fun onProductShippingClassSelected(resultCode: Int, shippingClass: WCProductShippingClassModel)
        fun onRequestProductShippingClasses(loadMore: Boolean = false)
    }

    private var resultCode: Int = -1
    private var selectedListItem: String? = null

    private var dialogTitle: String? = null
    private var adapter: ProductShippingClassAdapter? = null
    private var listener: ProductShippingClassSelectorDialogListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as ProductShippingClassSelectorDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(dialogTitle)
                .setView(R.layout.dialog_product_shipping_class_list)

        return builder.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ProductShippingClassAdapter(requireActivity(), this)
        recycler.adapter = adapter
        listener?.onRequestProductShippingClasses()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    /**
     * User made a selection in the shipping class adapter
     */
    override fun onShippingClassClicked(shippingClass: WCProductShippingClassModel) {
        listener?.onProductShippingClassSelected(resultCode, shippingClass)
    }
}
