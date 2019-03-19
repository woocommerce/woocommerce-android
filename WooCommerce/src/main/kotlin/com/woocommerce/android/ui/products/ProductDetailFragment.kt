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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
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
        PricingAndInventory,
        Inventory,
        Shipping,
        Attributes,
        Downloads,
        SalesAndReviews
    }

    @Inject lateinit var presenter: ProductDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus

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
        val product = presenter.getProduct(remoteProductId)
        if (product == null) {
            presenter.fetchProduct(remoteProductId)
        } else {
            showProduct(product)
            if (savedInstanceState == null) {
                presenter.fetchProduct(remoteProductId)
            }
        }
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

    override fun showFetchProductError() {
        uiMessageResolver.showSnack(R.string.product_detail_fetch_product_error)

        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
        }
    }

    override fun hideProgress() {
        if (loadingProgress.visibility != View.GONE) {
            loadingProgress.visibility = View.GONE
            productDetail_container.visibility = View.VISIBLE
        }
    }

    override fun showProgress() {
        if (loadingProgress.visibility != View.VISIBLE) {
            loadingProgress.visibility = View.VISIBLE
            productDetail_container.visibility = View.GONE
        }
    }

    override fun showProduct(product: WCProductModel) {
        if (!isAdded) return

        if (product.name.isNotEmpty()) {
            activity?.title = product.name
        }

        product.getFirstImageUrl()?.let {
            val imageWidth = DisplayUtils.getDisplayPixelWidth(activity)
            val imageHeight = resources.getDimensionPixelSize(R.dimen.product_detail_image_height)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageWidth, imageHeight)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .error(R.drawable.ic_product)
                    .placeholder(R.drawable.picture_frame)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(productDetail_image)
        }

        addPrimaryCard(product)
        addPricingAndInventoryCard(product)
        addAttributesCard(product)
        addDownloadCard(product)
        addShippingCard(product)
        addSalesAndReviewsCard(product)

        /**
         * TODO:
         *  - Need currency for price
         *  - Need dimension and weight units
         *  - Zoom product image when tapped
         *  - Show product detail from order product list
         *  - Product reviews
         *  - Linked products
         *  - Product variations
         *  - Skeletons
         */
    }

    private fun addPrimaryCard(product: WCProductModel) {
        addProperty(DetailCard.Primary, R.string.product_name, product.name)
        addProperty(DetailCard.Primary, R.string.product_description, product.description)
        addProperty(DetailCard.Primary, R.string.product_short_description, product.shortDescription)
        addProperty(DetailCard.Primary, R.string.product_purchase_note, product.purchaseNote)
        addProperty(DetailCard.Primary, R.string.product_categories, product.getCommaSeparatedCategoryNames())
        addProperty(DetailCard.Primary, R.string.product_tags, product.getCommaSeparatedTagNames())
        addProperty(DetailCard.Primary, R.string.product_catalog_visibility, product.catalogVisibility)
    }

    private fun addPricingAndInventoryCard(product: WCProductModel) {
        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.price.isNotEmpty() ||
                product.salePrice.isNotEmpty() ||
                product.taxClass.isNotEmpty() ||
                product.taxStatus.isNotEmpty()
        val pricingCard = if (hasPricingInfo) DetailCard.PricingAndInventory else DetailCard.Inventory
        if (hasPricingInfo) {
            addProperty(pricingCard, R.string.product_price, product.price)
            addProperty(pricingCard, R.string.product_sale_price, product.salePrice)
            addProperty(pricingCard, R.string.product_tax_status, product.taxStatus)
            addProperty(pricingCard, R.string.product_tax_class, product.taxClass)
        }
        addProperty(pricingCard, R.string.product_sku, product.sku)
        if (product.manageStock) {
            addProperty(pricingCard, R.string.product_stock_status, product.stockStatus)
            addProperty(pricingCard, R.string.product_backorders, product.backorders)
            addProperty(pricingCard, R.string.product_stock_quantity, product.stockQuantity.toString())
        }
    }

    private fun addAttributesCard(product: WCProductModel) {
        product.getAttributes().forEach { attribute ->
            addProperty(DetailCard.Attributes, attribute.name, attribute.getCommaSeparatedOptions())
        }
    }

    private fun addDownloadCard(product: WCProductModel) {
        if (product.downloadable) {
            addProperty(
                    DetailCard.Downloads,
                    R.string.product_downloadable_files,
                    product.getDownloadableFiles().size.toString()
            )
            if (product.downloadLimit > 0) {
                addProperty(DetailCard.Downloads, R.string.product_download_limit, product.downloadLimit.toString())
            }
            if (product.downloadExpiry > 0) {
                val expiryDays = String.format(
                        getString(R.string.product_download_expiry_days),
                        product.downloadExpiry
                )
                addProperty(DetailCard.Downloads, R.string.product_download_expiry, expiryDays)
            }
        }
    }

    private fun addShippingCard(product: WCProductModel) {
        addProperty(DetailCard.Shipping, R.string.product_weight, product.weight)
        addProperty(DetailCard.Shipping, R.string.product_length, product.length)
        addProperty(DetailCard.Shipping, R.string.product_width, product.width)
        addProperty(DetailCard.Shipping, R.string.product_height, product.height)
        addProperty(DetailCard.Shipping, R.string.product_shipping_class, product.shippingClass)
    }

    private fun addSalesAndReviewsCard(product: WCProductModel) {
        addProperty(DetailCard.SalesAndReviews, R.string.product_total_sales, product.totalSales.toString())
        if (product.ratingCount > 0) {
            addProperty(
                    DetailCard.SalesAndReviews,
                    R.string.product_average_rating,
                    product.averageRating
            )?.setRating(product.averageRating)
        }
        addProperty(DetailCard.SalesAndReviews, R.string.product_total_ratings, product.ratingCount.toString())
    }

    /**
     * Adds a WCCaptionedCardView to the current view if it doesn't already exist, then adds a WCCaptionedTextView
     * to the card if it doesn't already exist - this enables us to dynamically build the product detail and
     * more easily move things around.
     *
     * ex: addProperty(DetailCard.Pricing, R.string.product_price, product.price) will add the Pricing card if it
     * doesn't exist, and then add the product price caption and property to the card - but if the property
     * is empty, nothing gets added.
     */
    private fun addProperty(
        card: DetailCard,
        @StringRes propertyCaptionId: Int,
        propertyValue: String
    ): WCProductPropertyView? {
        return addProperty(card, getString(propertyCaptionId), propertyValue)
    }

    private fun addProperty(
        card: DetailCard,
        propertyCaption: String,
        propertyValue: String
    ): WCProductPropertyView? {
        if (propertyValue.isBlank() || view == null) return null

        val cardViewCaption: String? = when (card) {
            DetailCard.Primary -> null
            DetailCard.PricingAndInventory -> getString(R.string.product_pricing_and_inventory)
            DetailCard.Inventory -> getString(R.string.product_inventory)
            DetailCard.Shipping -> getString(R.string.product_shipping)
            DetailCard.Attributes -> getString(R.string.product_attributes)
            DetailCard.Downloads -> getString(R.string.product_downloads)
            DetailCard.SalesAndReviews -> getString(R.string.product_sales_and_reviews)
        }

        val context = activity as Context

        // find the cardView, add it if not found
        val cardTag = "${card.name}_tag"
        var cardView = productDetail_container.findViewWithTag<WCProductPropertyCardView>(cardTag)
        if (cardView == null) {
            // add a divider above the card if this isn't the first card
            if (card != DetailCard.Primary) {
                addCardDividerView(context)
            }
            cardView = WCProductPropertyCardView(context)
            cardView.elevation = (resources.getDimensionPixelSize(R.dimen.card_elevation)).toFloat()
            cardView.tag = cardTag
            cardView.show(cardViewCaption)
            productDetail_container.addView(cardView)
        }

        // locate the linear layout inside the cardView
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)

        // find the existing caption view in the card's linearLayout, add it if not found
        val captionTag = "{$propertyCaption}_tag"
        var captionedView = container.findViewWithTag<WCProductPropertyView>(captionTag)
        if (captionedView == null) {
            captionedView = WCProductPropertyView(context)
            captionedView.tag = captionTag
            container.addView(captionedView)
        }

        val orientation = when (card) {
            DetailCard.Primary,
            DetailCard.Attributes -> LinearLayout.VERTICAL
            else ->
                LinearLayout.HORIZONTAL
        }

        // some details, such as product description, contain html which needs to be stripped here
        captionedView.show(orientation, propertyCaption, HtmlUtils.fastStripHtml(propertyValue).trim())
        return captionedView
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
