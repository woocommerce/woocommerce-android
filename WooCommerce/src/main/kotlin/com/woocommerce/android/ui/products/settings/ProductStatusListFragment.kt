package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductStatus.PENDING
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import kotlinx.android.synthetic.main.fragment_product_status_list.*

/**
 * Settings screen which enables choosing a product status
 */
class ProductStatusListFragment : BaseFragment() {
    companion object {
        const val ARG_SELECTED_STATUS = "selected_status"
    }

    private val navArgs: ProductStatusListFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_status_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getButtonForStatus(navArgs.status)?.isChecked = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            getStatusForButtonId(checkedId)?.let { status ->
                val bundle = Bundle().also {
                    it.putSerializable(ARG_SELECTED_STATUS, status)
                }
                requireActivity().navigateBackWithResult(
                        RequestCodes.PRODUCT_SETTINGS_STATUS,
                        bundle,
                        R.id.nav_host_fragment_main,
                        R.id.productSettingsFragment
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_status)

    private fun getButtonForStatus(status: String): RadioButton? {
        return when (ProductStatus.fromString(status)) {
            PUBLISH -> btnPublished
            DRAFT -> btnDraft
            PENDING -> btnPending
            PRIVATE -> btnPrivate
            else -> null
        }
    }

    private fun getStatusForButtonId(@IdRes buttonId: Int): ProductStatus? {
        return when (buttonId) {
            R.id.btnPublished -> PUBLISH
            R.id.btnDraft -> DRAFT
            R.id.btnPending -> PENDING
            R.id.btnPrivate -> PRIVATE
            else -> null
        }
    }
}
