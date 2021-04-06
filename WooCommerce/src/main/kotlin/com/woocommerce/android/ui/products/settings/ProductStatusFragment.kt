package com.woocommerce.android.ui.products.settings

import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.CheckedTextView
import androidx.annotation.IdRes
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductStatusBinding
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductStatus.PENDING
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductStatus.TRASH

/**
 * Settings screen which enables choosing a product status
 */
class ProductStatusFragment : BaseProductSettingsFragment(R.layout.fragment_product_status), OnClickListener {
    companion object {
        const val ARG_SELECTED_STATUS = "selected_status"
    }

    private val navArgs: ProductStatusFragmentArgs by navArgs()
    private lateinit var selectedStatus: String

    private var _binding: FragmentProductStatusBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedStatus = savedInstanceState?.getString(ARG_SELECTED_STATUS) ?: navArgs.status
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProductStatusBinding.bind(view)

        binding.btnPublished.setOnClickListener(this)
        binding.btnPublishedPrivately.setOnClickListener(this)
        binding.btnDraft.setOnClickListener(this)
        binding.btnPending.setOnClickListener(this)
        binding.btnTrashed.setOnClickListener(this)

        getButtonForStatus(selectedStatus)?.isChecked = true

        // if the post is private, we hide the "Published" button and show "Privately published."
        // making a product private is done on the product visibility screen
        if (selectedStatus == PRIVATE.toString()) {
            binding.btnPublishedPrivately.visibility = View.VISIBLE
            binding.btnPublished.visibility = View.GONE
        } else {
            binding.btnPublishedPrivately.visibility = View.GONE
            binding.btnPublished.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_SELECTED_STATUS, selectedStatus)
    }

    override fun onClick(view: View?) {
        (view as? CheckedTextView)?.let {
            binding.btnPublished.isChecked = it == binding.btnPublished
            binding.btnPublishedPrivately.isChecked = it == binding.btnPublishedPrivately
            binding.btnDraft.isChecked = it == binding.btnDraft
            binding.btnPending.isChecked = it == binding.btnPending
            binding.btnTrashed.isChecked = it == binding.btnTrashed
            selectedStatus = getStatusForButtonId(it.id)
        }
    }

    override fun getChangesResult() = ARG_SELECTED_STATUS to ProductStatus.fromString(selectedStatus)!!

    override fun hasChanges() = navArgs.status != selectedStatus

    override fun validateChanges() = true

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_status)

    private fun getButtonForStatus(status: String): CheckedTextView? {
        return when (ProductStatus.fromString(status)) {
            PUBLISH -> binding.btnPublished
            DRAFT -> binding.btnDraft
            PENDING -> binding.btnPending
            PRIVATE -> binding.btnPublishedPrivately
            TRASH -> binding.btnTrashed
            else -> null
        }
    }

    private fun getStatusForButtonId(@IdRes buttonId: Int): String {
        return when (buttonId) {
            R.id.btnPublished -> PUBLISH.toString()
            R.id.btnDraft -> DRAFT.toString()
            R.id.btnPending -> PENDING.toString()
            R.id.btnPublishedPrivately -> PRIVATE.toString()
            R.id.btnTrashed -> TRASH.toString()
            else -> throw IllegalArgumentException()
        }
    }
}
