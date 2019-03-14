package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v7.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.WCCaptionedTextView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

class ProductDetailFragment : Fragment(), ProductDetailContract.View {
    companion object {
        const val TAG = "ProductDetailFragment"
        private const val ARG_REMOTE_PRODUCT_ID = "remote_product_id"
        private const val CARD_TAG_PRIMARY = "card_primary"
        private const val CARD_LINEAR_LAYOUT_TAG = "linearlayout"

        fun newInstance(remoteProductId: Long): Fragment {
            val args = Bundle()
            args.putLong(ARG_REMOTE_PRODUCT_ID, remoteProductId)
            val fragment = ProductDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: ProductDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var productImageMap: ProductImageMap

    private var runOnStartFunc: (() -> Unit)? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        val remoteProductId = arguments?.getLong(ARG_REMOTE_PRODUCT_ID) ?: 0L
        presenter.getProduct(remoteProductId)?.let { product ->
            showProduct(product)
        } ?: showProgress()
        presenter.fetchProduct(remoteProductId)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStart() {
        super.onStart()

        runOnStartFunc?.let {
            it.invoke()
            runOnStartFunc = null
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showProduct(product: WCProductModel) {
        loadingProgress.visibility = View.GONE

        product.getFirstImageUrl()?.let {
            val imageWidth = DisplayUtils.getDisplayPixelWidth(activity)
            val imageHeight = resources.getDimensionPixelSize(R.dimen.product_image_height)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageWidth, imageHeight)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .error(R.drawable.ic_product)
                    .into(productDetail_image)
        }

        addView(CARD_TAG_PRIMARY, R.string.product_name, product.name)
        addView(CARD_TAG_PRIMARY, R.string.product_description, product.description)
        addView(CARD_TAG_PRIMARY, R.string.product_short_description, product.shortDescription)
        addView(CARD_TAG_PRIMARY, R.string.product_purchase_note, product.purchaseNote)
        addView(CARD_TAG_PRIMARY, R.string.product_categories, product.getCommaSeparatedCategoryNames())
        addView(CARD_TAG_PRIMARY, R.string.product_tags, product.getCommaSeparatedTagNames())
        addView(CARD_TAG_PRIMARY, R.string.product_catalog_visibility, product.catalogVisibility)
    }

    override fun showFetchProductError() {
        loadingProgress.visibility = View.GONE
        uiMessageResolver.showSnack(R.string.product_detail_fetch_product_error)

        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
        }
    }

    override fun hideProgress() {
        loadingProgress.visibility = View.GONE
    }

    override fun showProgress() {
        loadingProgress.visibility = View.VISIBLE
    }

    /**
     * Adds a WCCaptionedTextView to the current view if it doesn't already exist and the passed detail isn't empty
     */
    private fun addView(cardViewTag: String, @StringRes captionId: Int, detail: String): WCCaptionedTextView? {
        if (detail.isBlank() || view == null) return null

        val context = activity as Context

        // find the parent card, add it if not found
        var cardView = productDetail_container.findViewWithTag<CardView>(cardViewTag)
        if (cardView == null) {
            cardView = CardView(context)
            cardView.elevation = (resources.getDimensionPixelSize(R.dimen.card_elevation)).toFloat()
            cardView.tag = cardViewTag
            productDetail_container.addView(cardView)
        }

        // find the LinearLayout inside the parent card, add it if not found
        var linearLayout = cardView.findViewWithTag<LinearLayout>(CARD_LINEAR_LAYOUT_TAG)
        if (linearLayout == null) {
            linearLayout = LinearLayout(context)
            linearLayout.setOrientation(LinearLayout.VERTICAL)
            linearLayout.tag = CARD_LINEAR_LAYOUT_TAG
            cardView.addView(linearLayout)
        }

        val showTopDivider = linearLayout.childCount > 0

        // find the existing caption view, if not found then add it
        var captionedView = linearLayout.findViewWithTag<WCCaptionedTextView>(captionId.toString())
        if (captionedView == null) {
            captionedView = WCCaptionedTextView(context)
            linearLayout.addView(captionedView)
        }

        // some details, such as product description, contain html which needs to be stripped here
        val detailPlainText = HtmlUtils.fastStripHtml(detail).trim()
        captionedView.show(getString(captionId), detailPlainText, showTopDivider)

        // tag the view so we can use findViewWithTag above
        captionedView.tag = captionId.toString()

        return captionedView
    }
}
