package com.woocommerce.android.ui.products

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

/**
 * Dialog displays a list of product shipping classes
 *
 * This fragment should be instantiated using the [ProductShippingClassDialog.newInstance] method.
 * Calling classes can obtain the results of selection through the [onActivityResult]
 * via [ProductShippingClassDialog.getTargetFragment].
 */
class ProductShippingClassDialog : DialogFragment() {
    companion object {
        const val TAG: String = "ProductShippingClassDialog"

        fun newInstance(listener: Fragment): ProductShippingClassDialog {
            return ProductShippingClassDialog().also { fragment ->
                fragment.setTargetFragment(listener, RequestCodes.PRODUCT_SHIPPING_CLASS)
                fragment.retainInstance = true
            }
        }
    }

    interface ShippingClassDialogListener {
        fun onShippingClassClicked(shippingClass: WCProductShippingClassModel)
        fun onRequestShippingClasses(loadMore: Boolean = false)
        fun onShippingClassDialogCancelled()
    }

    private var recycler: RecyclerView? = null
    private var adapter: ProductShippingClassAdapter? = null
    private lateinit var listener: ShippingClassDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as ShippingClassDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(requireActivity(), R.layout.dialog_product_shipping_class_list, null)
        recycler = view.findViewById(R.id.recycler)
        recycler?.layoutManager = LinearLayoutManager(requireActivity())
        recycler?.addItemDecoration(
                DividerItemDecoration(
                        requireActivity(),
                        DividerItemDecoration.VERTICAL
                )
        )

        return AlertDialog.Builder(requireContext()).also { builder ->
            builder.setView(view)
        }.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ProductShippingClassAdapter(requireActivity(), listener)
        recycler?.adapter = adapter
        listener.onRequestShippingClasses()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        listener.onShippingClassDialogCancelled()
    }

    fun setShippingClasses(shippingClasses: List<WCProductShippingClassModel>) {
        adapter?.shippingClassList = shippingClasses
    }
}
