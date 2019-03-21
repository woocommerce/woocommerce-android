package com.woocommerce.android.ui.products

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.woocommerce.android.widgets.SkeletonView
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
        Attributes,
        Downloads,
        PurchaseDetails
    }

    @Inject lateinit var presenter: ProductDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus

    private var remoteProductId = 0L
    private var runOnStartFunc: (() -> Unit)? = null
    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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

        remoteProductId = arguments?.getLong(ARG_REMOTE_PRODUCT_ID) ?: 0L
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

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_share, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return if (item?.itemId == R.id.menu_share) {
            shareProduct()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun showFetchProductError() {
        uiMessageResolver.showSnack(R.string.product_detail_fetch_product_error)

        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
        }
    }

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(productDetail_container, R.layout.skeleton_product_detail, delayed = true)
        } else {
            skeletonView.hide()
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
        addPurchaseDetailsCard(product)
    }

    private fun addPrimaryCard(product: WCProductModel) {
        addProperty(DetailCard.Primary, R.string.product_name, product.name, LinearLayout.VERTICAL)
        addProperty(DetailCard.Primary, R.string.product_total_orders, product.totalSales.toString())

        if (product.ratingCount > 0) {
            addProperty(
                    DetailCard.Primary,
                    R.string.product_reviews,
                    product.ratingCount.toString()
            )?.setRating(product.averageRating)
        }
    }

    private fun addPricingAndInventoryCard(product: WCProductModel) {
        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.regularPrice.isNotEmpty() ||
                product.salePrice.isNotEmpty() ||
                product.taxClass.isNotEmpty() ||
                product.taxStatus.isNotEmpty()
        val pricingCard = if (hasPricingInfo) DetailCard.PricingAndInventory else DetailCard.Inventory

        if (hasPricingInfo) {
            // when there's a sale price show price & sales price as a group, otherwise show price separately
            if (product.salePrice.isNotEmpty()) {
                val group = mapOf(
                        Pair(getString(R.string.product_regular_price), product.regularPrice),
                        Pair(getString(R.string.product_sale_price), product.salePrice)
                )
                addPropertyGroup(pricingCard, R.string.product_price, group)
            } else {
                addProperty(pricingCard, R.string.product_price, product.price)
            }
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        if (product.manageStock) {
            val group = mapOf(
                    Pair(getString(R.string.product_stock_status), product.stockStatus),
                    Pair(getString(R.string.product_backorders), product.backorders),
                    Pair(getString(R.string.product_stock_quantity), product.stockQuantity.toString()),
                    Pair(getString(R.string.product_sku), product.sku)
            )
            addPropertyGroup(pricingCard, R.string.product_inventory, group)
        } else {
            addProperty(pricingCard, R.string.product_sku, product.sku)
        }
    }

    private fun addAttributesCard(product: WCProductModel) {
        product.getAttributes().forEach { attribute ->
            addProperty(
                    DetailCard.Attributes,
                    attribute.name,
                    attribute.getCommaSeparatedOptions(),
                    LinearLayout.VERTICAL
            )
        }
    }

    private fun addDownloadCard(product: WCProductModel) {
        if (!product.downloadable) return

        addProperty(
                DetailCard.Downloads,
                R.string.product_downloadable_files,
                product.getDownloadableFiles().size.toString()
        )
        if (product.downloadLimit > 0) {
            addProperty(DetailCard.Downloads, R.string.product_download_limit, product.downloadLimit.toString())
        }
        if (product.downloadExpiry > 0) {
            val expiryDays = String.format(getString(R.string.product_download_expiry_days), product.downloadExpiry)
            addProperty(DetailCard.Downloads, R.string.product_download_expiry, expiryDays)
        }
    }

    private fun addPurchaseDetailsCard(product: WCProductModel) {
        val hasLength = product.length.isNotEmpty()
        val hasWidth = product.width.isNotEmpty()
        val hasHeight = product.height.isNotEmpty()

        // l x w x h
        val propertySize = if (hasLength && hasWidth && hasHeight) {
            "${product.length} x ${product.width} x ${product.height}"
        } else if (hasWidth && hasHeight) {
            "${product.width} x ${product.height}"
        } else {
            ""
        }

        val group = mapOf(
                Pair(getString(R.string.product_weight), product.weight),
                Pair(getString(R.string.product_size), propertySize),
                Pair(getString(R.string.product_shipping_class), product.shippingClass)
        )
        addPropertyGroup(DetailCard.PurchaseDetails, R.string.product_shipping, group)

        addProperty(
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
     * ex: addProperty(DetailCard.Pricing, R.string.product_price, product.price) will add the Pricing card if it
     * doesn't exist, and then add the product price caption and property to the card - but if the property
     * is empty, nothing gets added.
     */
    private fun addProperty(
        card: DetailCard,
        @StringRes propertyNameId: Int,
        propertyValue: String,
        orientation: Int = LinearLayout.HORIZONTAL
    ): WCProductPropertyView? {
        return addProperty(card, getString(propertyNameId), propertyValue, orientation)
    }

    private fun addProperty(
        card: DetailCard,
        propertyName: String,
        propertyValue: String,
        orientation: Int = LinearLayout.HORIZONTAL
    ): WCProductPropertyView? {
        if (propertyValue.isBlank() || view == null) return null

        val cardViewCaption: String? = when (card) {
            DetailCard.Primary -> null
            DetailCard.PricingAndInventory -> getString(R.string.product_pricing_and_inventory)
            DetailCard.Inventory -> getString(R.string.product_inventory)
            DetailCard.Attributes -> getString(R.string.product_attributes)
            DetailCard.Downloads -> getString(R.string.product_downloads)
            DetailCard.PurchaseDetails -> getString(R.string.product_purchase_details)
        }

        val context = activity as Context

        // find the card, add it if not found
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

        // locate the linear layout container inside the card
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)

        // find the existing property view in the container, add it if not found
        val propertyTag = "{$propertyName}_tag"
        var propertyView = container.findViewWithTag<WCProductPropertyView>(propertyTag)
        if (propertyView == null) {
            propertyView = WCProductPropertyView(context)
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
        @StringRes groupNameId: Int,
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
        return addProperty(card, groupNameId, propertyValue, LinearLayout.VERTICAL)
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
            activity?.startActivity(Intent.createChooser(shareIntent, title))
        }
    }
}
