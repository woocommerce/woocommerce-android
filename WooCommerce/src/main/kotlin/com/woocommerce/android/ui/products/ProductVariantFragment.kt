package com.woocommerce.android.ui.products

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ProductVariantViewModel.VariationExitEvent.ExitVariation
import com.woocommerce.android.ui.products.ProductVariantViewModel.ShowVariantImage
import com.woocommerce.android.ui.products.adapters.ProductPropertyCardsAdapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.fragment_product_variant.*
import javax.inject.Inject

class ProductVariantFragment : BaseFragment(), BackPressListener {
    companion object {
        private const val LIST_STATE_KEY = "list_state"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory

    private var variationName = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    private val skeletonView = SkeletonView()
    private var progressDialog: CustomProgressDialog? = null
    private var layoutManager: LayoutManager? = null

    private val viewModel: ProductVariantViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_variant, container, false)
    }

    override fun onDestroyView() {
        // hide the skeleton view if fragment is destroyed
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(savedInstanceState)
        initializeViewModel()
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        this.layoutManager = layoutManager

        savedInstanceState?.getParcelable<Parcelable>(LIST_STATE_KEY)?.let {
            layoutManager.onRestoreInstanceState(it)
        }
        cardsRecyclerView.layoutManager = layoutManager
        cardsRecyclerView.itemAnimator = null
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductVariantViewModel) {
        viewModel.variantViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.variant?.takeIfNotEqualTo(old?.variant) { showVariationDetails(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
        }

        viewModel.variantDetailCards.observe(viewLifecycleOwner, Observer {
            showVariationCards(it)
        })

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowVariantImage -> {
                    val action = ProductVariantFragmentDirections.actionGlobalWpMediaViewerFragment(event.image.source)
                    findNavController().navigateSafely(action)
                }
                else -> event.isHandled = false
            }
        })
    }

    private fun showVariationDetails(variation: ProductVariant) {
        variationName = variation.optionName.fastStripHtml()

        if (variation.image == null) {
            variationImage.visibility = View.GONE
        } else {
            variationImage.visibility = View.VISIBLE
            GlideApp.with(this)
                .load(variation.image.source)
                .placeholder(R.drawable.ic_product)
                .into(variationImage)
            variationImage.setOnClickListener {
                viewModel.onVariantImageClicked()
            }
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(app_bar_layout, R.layout.skeleton_variant_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.product_update_dialog_title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showVariationCards(cards: List<ProductPropertyCard>) {
        val adapter: ProductPropertyCardsAdapter
        if (cardsRecyclerView.adapter == null) {
            adapter = ProductPropertyCardsAdapter()
            cardsRecyclerView.adapter = adapter
        } else {
            adapter = cardsRecyclerView.adapter as ProductPropertyCardsAdapter
        }

        val recyclerViewState = cardsRecyclerView.layoutManager?.onSaveInstanceState()
        adapter.update(cards)
        cardsRecyclerView.layoutManager?.onRestoreInstanceState(recyclerViewState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        layoutManager?.let {
            outState.putParcelable(LIST_STATE_KEY, it.onSaveInstanceState())
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked(ExitVariation())
    }

    override fun getFragmentTitle() = variationName
}
