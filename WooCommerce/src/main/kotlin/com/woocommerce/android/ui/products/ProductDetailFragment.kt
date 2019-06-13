package com.woocommerce.android.ui.products

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_SHARE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_AFFILIATE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.imageviewer.ImageViewerActivity
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

class ProductDetailFragment : androidx.fragment.app.Fragment(), ProductDetailContract.View, RequestListener<Drawable> {
    private enum class DetailCard {
        Primary,
        PricingAndInventory,
        Inventory,
        PurchaseDetails
    }

    @Inject lateinit var presenter: ProductDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus

    private var productTitle = ""
    private var activityTitle = ""
    private var productImageUrl: String? = null
    private var isVariation = false
    private var imageHeight = 0
    private val skeletonView = SkeletonView()

    val navArgs: ProductDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        activity?.let {
            activityTitle = it.title.toString()
            (it as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        }

        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onDestroyView() {
        activity?.let {
            it.title = activityTitle
            (it as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_white_24dp)
        }
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // make image height a percentage of screen height, adjusting for landscape
        val displayHeight = DisplayUtils.getDisplayPixelHeight(activity!!)
        val multiplier = if (DisplayUtils.isLandscape(activity!!)) 0.5f else 0.3f
        imageHeight = (displayHeight * multiplier).toInt()
        productDetail_image.layoutParams.height = imageHeight

        // set the height of the gradient scrim that appears atop the image
        imageScrim.layoutParams.height = imageHeight / 3

        presenter.takeView(this)
        presenter.loadProductDetail(navArgs.remoteProductId)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.menu_share, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when {
            item?.itemId == R.id.menu_share -> {
                AnalyticsTracker.track(PRODUCT_DETAIL_SHARE_BUTTON_TAPPED)
                shareProduct()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showFetchProductError() {
        uiMessageResolver.showSnack(R.string.product_detail_fetch_product_error)
        activity?.onBackPressed()
    }

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productDetail_root, R.layout.skeleton_product_detail, delayed = true)
            skeletonView.findViewById(R.id.productImage_Skeleton)?.layoutParams?.height = imageHeight
        } else {
            skeletonView.hide()
        }
    }

    override fun showProduct(product: WCProductModel) {
        if (!isAdded) return

        productTitle = when (ProductType.fromString(product.type)) {
            EXTERNAL -> getString(R.string.product_name_external, product.name)
            GROUPED -> getString(R.string.product_name_grouped, product.name)
            VARIABLE -> getString(R.string.product_name_variable, product.name)
            else -> {
                if (product.virtual) {
                    getString(R.string.product_name_virtual, product.name)
                } else {
                    product.name
                }
            }
        }

        activity?.title = productTitle

        isVariation = ProductType.fromString(product.type) == ProductType.VARIATION

        val imageUrl = product.getFirstImageUrl()
        if (imageUrl != null) {
            val width = DisplayUtils.getDisplayPixelWidth(activity!!)
            val height = DisplayUtils.getDisplayPixelHeight(activity!!)
            val imageSize = Math.max(width, height)
            productImageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, 0)
            GlideApp.with(activity!!)
                    .load(productImageUrl)
                    .error(R.drawable.ic_product)
                    .placeholder(R.drawable.product_detail_image_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(this)
                    .into(productDetail_image)
        } else {
            productDetail_image.visibility = View.GONE
            imageScrim.visibility = View.GONE
        }

        // show status badge for unpublished products
        ProductStatus.fromString(product.status)?.let { status ->
            if (status != ProductStatus.PUBLISH) {
                frameStatusBadge.visibility = View.VISIBLE
                textStatusBadge.text = status.toString(activity!!)
            }
        }

        addPrimaryCard(product)
        addPricingAndInventoryCard(product)
        addPurchaseDetailsCard(product)
    }

    private fun addPrimaryCard(product: WCProductModel) {
        addPropertyView(DetailCard.Primary, R.string.product_name, productTitle, LinearLayout.VERTICAL)

        // we don't show total sales for variations because they're always zero
        if (!isVariation) {
            addPropertyView(
                    DetailCard.Primary,
                    R.string.product_total_orders,
                    StringUtils.formatCount(product.totalSales)
            )
        }

        // we don't show reviews for variations because they're always empty
        if (!isVariation && product.reviewsAllowed) {
            addPropertyView(
                    DetailCard.Primary,
                    R.string.product_reviews,
                    StringUtils.formatCount(product.ratingCount)
            )?.setRating(product.averageRating)
        }

        addLinkView(
                DetailCard.Primary,
                R.string.product_view_in_store,
                product.permalink,
                PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED
        )
        addLinkView(
                DetailCard.Primary,
                R.string.product_view_affiliate,
                product.externalUrl,
                PRODUCT_DETAIL_VIEW_AFFILIATE_TAPPED
        )
    }

    private fun addPricingAndInventoryCard(product: WCProductModel) {
        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.price.isNotEmpty() ||
                product.salePrice.isNotEmpty() ||
                product.taxClass.isNotEmpty()
        val pricingCard = if (hasPricingInfo) DetailCard.PricingAndInventory else DetailCard.Inventory

        if (hasPricingInfo) {
            // when there's a sale price show price & sales price as a group, otherwise show price separately
            if (product.salePrice.isNotEmpty()) {
                val group = mapOf(
                        Pair(getString(R.string.product_regular_price), presenter.formatCurrency(product.regularPrice)),
                        Pair(getString(R.string.product_sale_price), presenter.formatCurrency(product.salePrice))
                )
                addPropertyGroup(pricingCard, R.string.product_price, group)
            } else {
                addPropertyView(
                        pricingCard,
                        R.string.product_price,
                        presenter.formatCurrency(product.price),
                        LinearLayout.VERTICAL
                )
            }
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        if (product.manageStock) {
            val group = mapOf(
                    Pair(getString(R.string.product_stock_status), stockStatusToDisplayString(product.stockStatus)),
                    Pair(getString(R.string.product_backorders), backordersToDisplayString(product.backorders)),
                    Pair(getString(R.string.product_stock_quantity), StringUtils.formatCount(product.stockQuantity)),
                    Pair(getString(R.string.product_sku), product.sku)
            )
            addPropertyGroup(pricingCard, R.string.product_inventory, group)
        } else {
            addPropertyView(pricingCard, R.string.product_sku, product.sku, LinearLayout.VERTICAL)
        }
    }

    private fun addPurchaseDetailsCard(product: WCProductModel) {
        val hasLength = product.length.isNotEmpty()
        val hasWidth = product.width.isNotEmpty()
        val hasHeight = product.height.isNotEmpty()

        val dimensionUnit = presenter.getDimensionUnit()
        val propertySize = if (hasLength && hasWidth && hasHeight) {
            "${product.length} x ${product.width} x ${product.height} $dimensionUnit"
        } else if (hasWidth && hasHeight) {
            "${product.width} x ${product.height} $dimensionUnit"
        } else {
            ""
        }

        val weightUnit = presenter.getWeightUnit()
        val weight = if (product.weight.isNotEmpty()) "${product.weight}$weightUnit" else ""

        val shippingGroup = mapOf(
                Pair(getString(R.string.product_weight), weight),
                Pair(getString(R.string.product_size), propertySize),
                Pair(getString(R.string.product_shipping_class), product.shippingClass)
        )
        addPropertyGroup(DetailCard.PurchaseDetails, R.string.product_shipping, shippingGroup)

        if (product.downloadable) {
            val count = product.getDownloadableFiles().size.toString()
            val limit = if (product.downloadLimit > 0) String.format(
                    getString(R.string.product_download_limit_count),
                    product.downloadLimit
            ) else ""
            val expiry = if (product.downloadExpiry > 0) String.format(
                    getString(R.string.product_download_expiry_days),
                    product.downloadExpiry
            ) else ""

            val downloadGroup = mapOf(
                    Pair(getString(R.string.product_downloadable_files), count),
                    Pair(getString(R.string.product_download_limit), limit),
                    Pair(getString(R.string.product_download_expiry), expiry)
            )
            addPropertyGroup(DetailCard.PurchaseDetails, R.string.product_downloads, downloadGroup)
        }

        if (product.purchaseNote.isNotBlank()) {
            addReadMoreView(
                    DetailCard.PurchaseDetails,
                    R.string.product_purchase_note,
                    product.purchaseNote,
                    2
            )
        }
    }

    /**
     * Adds a property card to the current view if it doesn't already exist, then adds the property & value
     * to the card if they don't already exist - this enables us to dynamically build the product detail and
     * more easily move things around.
     *
     * ex: addPropertyView(DetailCard.Pricing, R.string.product_price, product.price) will add the Pricing card if it
     * doesn't exist, and then add the product price caption and property to the card - but if the property
     * is empty, nothing gets added.
     */
    private fun addPropertyView(
        card: DetailCard,
        @StringRes propertyNameId: Int,
        propertyValue: String,
        orientation: Int = LinearLayout.HORIZONTAL
    ): WCProductPropertyView? {
        return addPropertyView(card, getString(propertyNameId), propertyValue, orientation)
    }

    private fun addPropertyView(
        card: DetailCard,
        propertyName: String,
        propertyValue: String,
        orientation: Int = LinearLayout.HORIZONTAL
    ): WCProductPropertyView? {
        if (propertyValue.isBlank()) return null

        // locate the card, add it if it doesn't exist yet
        val cardView = findOrAddCardView(card)

        // locate the linear layout container inside the card
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)

        // locate the existing property view in the container, add it if not found
        val propertyTag = "{$propertyName}_tag"
        var propertyView = container.findViewWithTag<WCProductPropertyView>(propertyTag)
        if (propertyView == null) {
            propertyView = WCProductPropertyView(activity!!)
            propertyView.tag = propertyTag
            container.addView(propertyView)
        }

        // some details, such as product description, contain html which needs to be stripped here
        propertyView.show(orientation, propertyName, HtmlUtils.fastStripHtml(propertyValue).trim())
        return propertyView
    }

    /**
     * Adds a group of related properties as a single property view
     */
    private fun addPropertyGroup(
        card: DetailCard,
        @StringRes groupTitleId: Int,
        properties: Map<String, String>
    ): WCProductPropertyView? {
        var propertyValue = ""
        properties.forEach { property ->
            if (property.value.isNotEmpty()) {
                if (propertyValue.isNotEmpty()) {
                    propertyValue += "\n"
                }
                propertyValue += "${property.key}: ${property.value}"
            }
        }
        return addPropertyView(card, getString(groupTitleId), propertyValue, LinearLayout.VERTICAL)
    }

    /**
     * Adds a property link to the passed card
     */
    private fun addLinkView(
        card: DetailCard,
        @StringRes captionId: Int,
        url: String,
        tracksEvent: Stat
    ): WCProductPropertyLinkView? {
        if (url.isEmpty()) return null

        val caption = getString(captionId)
        val linkViewTag = "${caption}_tag"

        val cardView = findOrAddCardView(card)
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)
        var linkView = container.findViewWithTag<WCProductPropertyLinkView>(linkViewTag)

        if (linkView == null) {
            linkView = WCProductPropertyLinkView(activity!!)
            linkView.tag = linkViewTag
            container.addView(linkView)
        }

        linkView.show(caption, url, tracksEvent)
        return linkView
    }

    /**
     * Adds a "read more" view which limits content to a certain number of lines, and if it goes over
     * a "Read more" button appears
     */
    private fun addReadMoreView(card: DetailCard, @StringRes captionId: Int, content: String, maxLines: Int) {
        val caption = getString(captionId)
        val readMoreTag = "${caption}_read_more_tag"

        val cardView = findOrAddCardView(card)
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)
        var readMoreView = container.findViewWithTag<WCProductPropertyReadMoreView>(readMoreTag)

        if (readMoreView == null) {
            readMoreView = WCProductPropertyReadMoreView(activity!!)
            readMoreView.tag = readMoreTag
            container.addView(readMoreView)
        }

        readMoreView.show(caption, HtmlUtils.fastStripHtml(content), maxLines)
    }

    /**
     * Returns the card view for the passed DetailCard - card will be added if it doesn't already exist
     */
    private fun findOrAddCardView(card: DetailCard): WCProductPropertyCardView {
        val cardTag = "${card.name}_tag"
        productDetail_container.findViewWithTag<WCProductPropertyCardView>(cardTag)?.let {
            return it
        }

        // add a divider above the card if this isn't the first card
        if (card != DetailCard.Primary) {
            addCardDividerView(activity!!)
        }

        val cardView = WCProductPropertyCardView(activity!!)
        cardView.tag = cardTag

        val cardViewCaption: String? = when (card) {
            DetailCard.Primary -> null
            DetailCard.PricingAndInventory -> getString(R.string.product_pricing_and_inventory)
            DetailCard.Inventory -> getString(R.string.product_inventory)
            DetailCard.PurchaseDetails -> getString(R.string.product_purchase_details)
        }

        cardView.show(cardViewCaption)
        productDetail_container.addView(cardView)

        return cardView
    }

    /**
     * Adds a divider between cards
     */
    private fun addCardDividerView(context: Context) {
        val divider = View(context)
        divider.layoutParams = LayoutParams(
                MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.product_detail_card_divider_height)
        )
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.default_window_background))
        productDetail_container.addView(divider)
    }

    private fun shareProduct() {
        presenter.getProduct(navArgs.remoteProductId)?.let { product ->
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, product.name)
                putExtra(Intent.EXTRA_TEXT, product.permalink)
                type = "text/plain"
            }
            val title = resources.getText(R.string.product_share_dialog_title)
            startActivity(Intent.createChooser(shareIntent, title))
        }
    }

    /**
     * returns the product's stock status formatted for display
     */
    private fun stockStatusToDisplayString(stockStatus: String?): String {
        return stockStatus?.let {
            when (it) {
                "instock" -> getString(R.string.product_stock_status_instock)
                "outofstock" -> getString(R.string.product_stock_status_out_of_stock)
                "onbackorder" -> getString(R.string.product_stock_status_on_backorder)
                else -> stockStatus
            }
        } ?: ""
    }

    private fun backordersToDisplayString(backorders: String?): String {
        return backorders?.let {
            when (it) {
                "no" -> getString(R.string.product_backorders_no)
                "yes" -> getString(R.string.product_backorders_yes)
                "notify" -> getString(R.string.product_backorders_notify)
                else -> backorders
            }
        } ?: ""
    }

    /**
     * Glide failed to load the product image, do nothing so Glide will show the error drawable
     */
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        return false
    }

    /**
     * Glide loaded the product image, add click listener to show image full screen
     */
    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        productImageUrl?.let { imageUrl ->
            productDetail_image.setOnClickListener {
                AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
                ImageViewerActivity.show(
                        activity!!,
                        imageUrl,
                        title = productTitle,
                        sharedElement = productDetail_image
                )
            }
        }
        return false
    }
}
