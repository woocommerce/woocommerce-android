package com.woocommerce.android.ui.products

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.imageviewer.ImageViewerActivity
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_product_detail.*
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import javax.inject.Inject

class ProductDetailActivity : AppCompatActivity(), ProductDetailContract.View, RequestListener<Drawable> {
    companion object {
        const val TAG = "ProductDetailFragment"
        private const val ARG_REMOTE_PRODUCT_ID = "remote_product_id"

        fun show(context: Context, remoteProductId: Long) {
            // TODO analytics
            val intent = Intent(context, ProductDetailActivity::class.java)
            intent.putExtra(ARG_REMOTE_PRODUCT_ID, remoteProductId)
            context.startActivity(intent)
        }
    }

    private enum class DetailCard {
        Primary,
        PricingAndInventory,
        Inventory,
        PurchaseDetails
    }

    @Inject lateinit var presenter: ProductDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus

    private var remoteProductId = 0L
    private var productName = ""
    private var productImageUrl: String? = null
    private var imageHeight = 0
    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_product_detail)

        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        adjustToolbar()

        // make image height a percentage of screen height, adjusting for landscape
        val displayHeight = DisplayUtils.getDisplayPixelHeight(this)
        val multiplier = if (DisplayUtils.isLandscape(this)) 0.6f else 0.4f
        imageHeight = (displayHeight * multiplier).toInt()
        productDetail_image.layoutParams.height = imageHeight

        // set the height of the gradient scrim that appears atop the image
        imageScrim.layoutParams.height = imageHeight / 3

        presenter.takeView(this)

        remoteProductId = savedInstanceState?.getLong(ARG_REMOTE_PRODUCT_ID) ?: intent.getLongExtra(
                ARG_REMOTE_PRODUCT_ID,
                0L
        )

        presenter.loadProductDetail(remoteProductId)

        // only show title when toolbar is collapsed
        app_bar_layout.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            var scrollRange = -1

            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.totalScrollRange
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsing_toolbar.title = productName
                } else {
                    collapsing_toolbar.title = " " // space between double quotes is on purpose
                }
            }
        })
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(ARG_REMOTE_PRODUCT_ID, remoteProductId)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_share, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when {
            item?.itemId == R.id.menu_share -> {
                shareProduct()
                true
            }
            item?.itemId == android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showFetchProductError() {
        uiMessageResolver.showSnack(R.string.product_detail_fetch_product_error)
        onBackPressed()
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
        if (isFinishing) return

        productName = product.name

        product.getFirstImageUrl()?.let {
            val width = DisplayUtils.getDisplayPixelWidth(this)
            val height = DisplayUtils.getDisplayPixelHeight(this)
            val imageSize = Math.max(width, height)
            productImageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, 0)
            GlideApp.with(this)
                    .load(productImageUrl)
                    .error(R.drawable.ic_product)
                    .placeholder(R.drawable.product_detail_image_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(this)
                    .into(productDetail_image)
        }

        // show status badge for unpublished products
        ProductStatus.fromString(product.status)?.let { status ->
            if (status != ProductStatus.PUBLISH) {
                textStatusBadge.visibility = View.VISIBLE
                textStatusBadge.text = status.toString(this)
            }
        }

        addPrimaryCard(product)
        addPricingAndInventoryCard(product)
        addPurchaseDetailsCard(product)
    }

    private fun addPrimaryCard(product: WCProductModel) {
        addPropertyView(DetailCard.Primary, R.string.product_name, product.name, LinearLayout.VERTICAL)
        addPropertyView(DetailCard.Primary, R.string.product_total_orders, StringUtils.formatCount(product.totalSales))
        if (product.ratingCount > 0) {
            addPropertyView(
                    DetailCard.Primary,
                    R.string.product_reviews,
                    StringUtils.formatCount(product.ratingCount)
            )?.setRating(product.averageRating)
        }

        addLinkView(DetailCard.Primary, R.string.product_view_in_store, product.permalink)
        addLinkView(DetailCard.Primary, R.string.product_view_affiliate, product.externalUrl)
    }

    private fun addPricingAndInventoryCard(product: WCProductModel) {
        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.price.isNotEmpty() ||
                product.salePrice.isNotEmpty() ||
                product.taxClass.isNotEmpty() ||
                product.taxStatus.isNotEmpty()
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
                addPropertyView(pricingCard, R.string.product_price, presenter.formatCurrency(product.price))
            }
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        if (product.manageStock) {
            val group = mapOf(
                    Pair(getString(R.string.product_stock_status), product.stockStatus),
                    Pair(getString(R.string.product_backorders), product.backorders),
                    Pair(getString(R.string.product_stock_quantity), StringUtils.formatCount(product.stockQuantity)),
                    Pair(getString(R.string.product_sku), product.sku)
            )
            addPropertyGroup(pricingCard, R.string.product_inventory, group)
        } else {
            addPropertyView(pricingCard, getString(R.string.product_sku), product.sku)
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

        addPropertyView(
                DetailCard.PurchaseDetails,
                R.string.product_purchase_note,
                product.purchaseNote,
                LinearLayout.VERTICAL
        )
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
            propertyView = WCProductPropertyView(this)
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
    private fun addLinkView(card: DetailCard, @StringRes captionId: Int, url: String): WCProductPropertyLinkView? {
        if (url.isEmpty()) return null

        val caption = getString(captionId)
        val linkViewTag = "${caption}_tag"

        val cardView = findOrAddCardView(card)
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)
        var linkView = container.findViewWithTag<WCProductPropertyLinkView>(linkViewTag)

        if (linkView == null) {
            linkView = WCProductPropertyLinkView(this)
            linkView.tag = linkViewTag
            container.addView(linkView)
        }

        linkView.show(caption, url)
        return linkView
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
            addCardDividerView(this)
        }

        val cardView = WCProductPropertyCardView(this)
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
        divider.setBackgroundColor(ContextCompat.getColor(context, R.color.list_divider))
        productDetail_container.addView(divider)
    }

    private fun shareProduct() {
        presenter.getProduct(remoteProductId)?.let { product ->
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

    /*
     * adjust the toolbar so it doesn't overlap the status bar
     */
    private fun adjustToolbar() {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            val statusHeight = resources.getDimensionPixelSize(resourceId)
            toolbar.layoutParams.height += statusHeight
            toolbar.setPadding(0, statusHeight, 0, 0)
        }
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
                ImageViewerActivity.show(this, imageUrl, title = productName, sharedElement = productDetail_image)
            }
        }
        return false
    }
}
