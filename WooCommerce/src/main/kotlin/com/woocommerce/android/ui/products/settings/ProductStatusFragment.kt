package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductStatus.PENDING
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import kotlinx.android.synthetic.main.fragment_product_status.*

/**
 * Settings screen which enables choosing a product status
 */
class ProductStatusFragment : BaseProductSettingsFragment(), OnClickListener {
    companion object {
        const val ARG_SELECTED_STATUS = "selected_status"
    }

    private val navArgs: ProductStatusFragmentArgs by navArgs()
    private var selectedStatus: String? = null

    override val requestCode = RequestCodes.PRODUCT_SETTINGS_STATUS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedStatus = savedInstanceState?.getString(ARG_SELECTED_STATUS) ?: navArgs.status
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnPublished.setOnClickListener(this)
        btnDraft.setOnClickListener(this)
        btnPending.setOnClickListener(this)
        btnPrivate.setOnClickListener(this)

        selectedStatus?.let {
            getButtonForStatus(it)?.isChecked = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_SELECTED_STATUS, selectedStatus)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            btnPublished.isChecked = it == btnPublished
            btnDraft.isChecked = it == btnDraft
            btnPending.isChecked = it == btnPending
            btnPrivate.isChecked = it == btnPrivate
            selectedStatus = getStatusForButtonId(it.id)
        }
    }

    override fun getChangesBundle(): Bundle {
        return Bundle().also {
            it.putString(ARG_SELECTED_STATUS, selectedStatus)
        }
    }

    override fun hasChanges() = navArgs.status != selectedStatus

    override fun validateChanges() = true

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_status)

    private fun getButtonForStatus(status: String): CheckedTextView? {
        return when (ProductStatus.fromString(status)) {
            PUBLISH -> btnPublished
            DRAFT -> btnDraft
            PENDING -> btnPending
            PRIVATE -> btnPrivate
            else -> null
        }
    }

    private fun getStatusForButtonId(@IdRes buttonId: Int): String? {
        return when (buttonId) {
            R.id.btnPublished -> PUBLISH.toString()
            R.id.btnDraft -> DRAFT.toString()
            R.id.btnPending -> PENDING.toString()
            R.id.btnPrivate -> PRIVATE.toString()
            else -> null
        }
    }
}
