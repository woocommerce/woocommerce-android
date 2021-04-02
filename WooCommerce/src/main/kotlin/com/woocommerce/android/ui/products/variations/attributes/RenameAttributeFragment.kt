package com.woocommerce.android.ui.products.variations.attributes

import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentAddAttributeTermsBinding
import com.woocommerce.android.databinding.FragmentRenameAttributeBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttributeTerms
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductRenameAttribute
import com.woocommerce.android.ui.products.variations.attributes.AttributeTermsListAdapter.OnTermListener
import com.woocommerce.android.widgets.AlignedDividerDecoration
import com.woocommerce.android.widgets.DraggableItemTouchHelper
import com.woocommerce.android.widgets.SkeletonView

class RenameAttributeFragment : BaseProductFragment(R.layout.fragment_rename_attribute) {
    companion object {
        const val TAG: String = "RenameAttributeFragment"
    }

    private var _binding: FragmentRenameAttributeBinding? = null
    private val binding get() = _binding!!

    private val navArgs: AddAttributeTermsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRenameAttributeBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        // TODO send the renamed attribute to the previous fragment
        return false
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = requireActivity().getString(R.string.product_rename_attribute)
}
