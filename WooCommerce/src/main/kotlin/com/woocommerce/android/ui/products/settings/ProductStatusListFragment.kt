package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductStatus.PENDING
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import kotlinx.android.synthetic.main.fragment_product_status_list.*

/**
 * Dialog which enables choosing a product status
 */
class ProductStatusListFragment : BaseProductFragment() {
    private val navArgs: ProductStatusListFragmentArgs by navArgs()

    override var shouldUpdateProductWhenEntering = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_status_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getButtonForStatus(navArgs.status)?.isChecked = true

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            getStatusForButtonId(checkedId)?.let { status ->
                viewModel.updateProductDraft(productStatus = status)
                findNavController().navigateUp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_status)

    override fun onRequestAllowBackPress() = true

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
