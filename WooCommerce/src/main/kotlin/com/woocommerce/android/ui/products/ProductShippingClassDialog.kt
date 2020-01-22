package com.woocommerce.android.ui.products

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.OnShippingClassClickListener
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

/**
 * Dialog displays a list of product shipping classes
 *
 * This fragment should be instantiated using the [ProductShippingClassDialog.newInstance] method.
 * Calling classes can obtain the results of selection through the [onActivityResult]
 * via [ProductShippingClassDialog.getTargetFragment].
 *
 * The [resultCode] passed to this fragment is used to classify the product shipping class
 */
class ProductShippingClassDialog : DialogFragment(), OnShippingClassClickListener {
    companion object {
        const val TAG: String = "ProductShippingSelectorDialog"

        fun newInstance(
            listener: Fragment,
            resultCode: Int
        ): ProductShippingClassDialog {
            return ProductShippingClassDialog().also { fragment ->
                fragment.setTargetFragment(listener, RequestCodes.PRODUCT_SHIPPING_CLASS)
                fragment.retainInstance = true
                fragment.resultCode = resultCode
            }
        }
    }

    interface ProductShippingClassDialogListener {
        fun onShippingClassSelected(resultCode: Int, shippingClass: WCProductShippingClassModel)
        fun onRequestShippingClasses(loadMore: Boolean = false)
    }

    private var resultCode: Int = -1
    private var recycler: RecyclerView? = null
    private var adapter: ProductShippingClassAdapter? = null
    private var listener: ProductShippingClassDialogListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as ProductShippingClassDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).also { builder ->
            builder.setTitle(R.string.product_shipping_class)
            builder.setView(R.layout.dialog_product_shipping_class_list)
        }.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ProductShippingClassAdapter(requireActivity(), this)
        recycler = dialog?.findViewById(R.id.recycler)
        recycler?.adapter = adapter
        listener?.onRequestShippingClasses()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    /**
     * User made a selection in the shipping class adapter
     */
    override fun onShippingClassClicked(shippingClass: WCProductShippingClassModel) {
        listener?.onShippingClassSelected(resultCode, shippingClass)
    }
}
