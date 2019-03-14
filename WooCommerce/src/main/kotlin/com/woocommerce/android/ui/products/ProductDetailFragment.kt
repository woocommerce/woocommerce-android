package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.WCCaptionedCardView
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

        fun newInstance(remoteProductId: Long): Fragment {
            val args = Bundle()
            args.putLong(ARG_REMOTE_PRODUCT_ID, remoteProductId)
            val fragment = ProductDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private enum class DetailCard {
        Primary,
        Pricing,
        Shipping
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
            val imageHeight = resources.getDimensionPixelSize(R.dimen.product_detail_image_height)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageWidth, imageHeight)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .error(R.drawable.ic_product)
                    .into(productDetail_image)
        }

        /**
         * TODO:
         *  - Need currency for price
         *  - Need dimension and weight units
         *  - Zoom product image when tapped
         *  - Toolbar caption
         *  - Show product detail from order product list
         */

        addView(DetailCard.Primary, R.string.product_name, product.name)
        addView(DetailCard.Primary, R.string.product_description, product.description)
        addView(DetailCard.Primary, R.string.product_short_description, product.shortDescription)
        addView(DetailCard.Primary, R.string.product_purchase_note, product.purchaseNote)
        addView(DetailCard.Primary, R.string.product_categories, product.getCommaSeparatedCategoryNames())
        addView(DetailCard.Primary, R.string.product_tags, product.getCommaSeparatedTagNames())
        addView(DetailCard.Primary, R.string.product_catalog_visibility, product.catalogVisibility)

        addView(DetailCard.Pricing, R.string.product_price, product.price)
        addView(DetailCard.Pricing, R.string.product_sale_price, product.salePrice)
        addView(DetailCard.Pricing, R.string.product_tax_status, product.taxStatus)
        addView(DetailCard.Pricing, R.string.product_tax_class, product.taxClass)
        addView(DetailCard.Pricing, R.string.product_sku, product.sku)
        addView(DetailCard.Pricing, R.string.product_stock_status, product.stockStatus)

        addView(DetailCard.Shipping, R.string.product_weight, product.weight)
        addView(DetailCard.Shipping, R.string.product_length, product.length)
        addView(DetailCard.Shipping, R.string.product_width, product.width)
        addView(DetailCard.Shipping, R.string.product_height, product.height)
        addView(DetailCard.Shipping, R.string.product_shipping_class, product.shippingClass)
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
     * Adds a WCCaptionedCardView to the current view if it doesn't already exist, then adds a WCCaptionedTextView
     * to the card if it doesn't already exist - this enables us to dynamically build the product detail and
     * more easily move things around
     */
    private fun addView(card: DetailCard, @StringRes captionId: Int, detail: String) {
        // don't do anything if detail is empty
        if (detail.isBlank() || view == null) return

        val context = activity as Context

        val cardViewCaption: String? = when (card) {
            DetailCard.Primary -> null
            DetailCard.Pricing -> getString(R.string.product_pricing_and_inventory)
            DetailCard.Shipping -> getString(R.string.product_shipping)
        }

        // find the cardView, add it if not found
        val cardViewTag = "${card.name}_tag"
        var cardView = productDetail_container.findViewWithTag<WCCaptionedCardView>(cardViewTag)
        if (cardView == null) {
            // add a divider above the card if this isn't the first card
            if (card != DetailCard.Primary) {
                addCardDividerView(context)
            }
            cardView = WCCaptionedCardView(context)
            cardView.elevation = (resources.getDimensionPixelSize(R.dimen.card_elevation)).toFloat()
            cardView.tag = cardViewTag
            cardView.show(cardViewCaption)
            productDetail_container.addView(cardView)
        }

        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)

        // find the existing caption view in the card, add it if not found
        val caption = getString(captionId)
        val captionTag = "{$caption}_tag"
        var captionedView = container.findViewWithTag<WCCaptionedTextView>(captionTag)
        if (captionedView == null) {
            captionedView = WCCaptionedTextView(context)
            captionedView.tag = captionTag
            container.addView(captionedView)
        }

        // some details, such as product description, contain html which needs to be stripped here
        val detailPlainText = HtmlUtils.fastStripHtml(detail).trim()
        captionedView.show(caption, detailPlainText)
    }

    private fun addCardDividerView(context: Context) {
        val divider = View(context)
        divider.layoutParams = LayoutParams(
                MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.product_detail_card_divider_height)
        )
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.list_divider))
        productDetail_container.addView(divider)
    }
}
