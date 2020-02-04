package com.woocommerce.android.ui.products

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_SHARE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_AFFILIATE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED
import com.woocommerce.android.extensions.formatToMMMdd
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.aztec.AztecEditorFragment
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.ARG_AZTEC_EDITOR_TEXT
import com.woocommerce.android.ui.aztec.AztecEditorFragment.Companion.AZTEC_EDITOR_REQUEST_CODE
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.imageviewer.ImageViewerActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.main.MainActivity.NavigationResult
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShareProduct
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShowImageChooser
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShowImages
import com.woocommerce.android.ui.products.ProductDetailViewModel.ViewState
import com.woocommerce.android.ui.products.ProductType.EXTERNAL
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CustomProgressDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCProductImageGalleryView.OnGalleryImageClickListener
import kotlinx.android.synthetic.main.fragment_product_detail.*
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import java.lang.ref.WeakReference
import java.util.Date
import javax.inject.Inject

class ProductDetailFragment : BaseFragment(), OnGalleryImageClickListener, NavigationResult,
        BackPressListener {
    private enum class DetailCard {
        Primary,
        Secondary,
        PricingAndInventory,
        Inventory,
        PurchaseDetails
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ProductDetailViewModel by viewModels { viewModelFactory }

    private var productTitle = ""
    private var isVariation = false
    private val skeletonView = SkeletonView()
    private var clickedImage = WeakReference<View>(null)

    private var publishMenuItem: MenuItem? = null

    private var progressDialog: CustomProgressDialog? = null

    private val navArgs: ProductDetailFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_product_detail, container, false)
    }

    override fun onDestroyView() {
        // hide the skeleton view if fragment is destroyed
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun onStop() {
        super.onStop()
        CustomDiscardDialog.onCleared()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViewModel()
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(navArgs.remoteProductId)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isProductUpdated?.takeIfNotEqualTo(old?.isProductUpdated) { showUpdateProductAction(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
            new.product?.takeIfNotEqualTo(old?.product) { showProduct(new) }
            new.uploadingImageUris?.takeIfNotEqualTo(old?.uploadingImageUris) {
                imageGallery.setPlaceholderImageUris(it)
            }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowImages -> showProductImages(event.product, event.image)
                is ShowImageChooser -> showImageChooser()
                is ShareProduct -> shareProduct(event.product)
                is Exit -> requireActivity().onBackPressed()
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                        requireActivity(),
                        event.positiveBtnAction,
                        event.negativeBtnAction
                )
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_product_detail_fragment, menu)
        publishMenuItem = menu.findItem(R.id.menu_update)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                AnalyticsTracker.track(PRODUCT_DETAIL_SHARE_BUTTON_TAPPED)
                viewModel.onShareButtonClicked()
                true
            }

            R.id.menu_update -> {
                AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED)
                ActivityUtils.hideKeyboard(activity)
                viewModel.onUpdateButtonClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestAllowBackPress(): Boolean {
        return viewModel.onBackButtonClicked()
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(app_bar_layout, R.layout.skeleton_product_detail, delayed = true)
            skeletonView.findViewById(R.id.productImage_Skeleton)?.layoutParams?.height = imageGallery.height
        } else {
            skeletonView.hide()
        }
    }

    private fun showUpdateProductAction(show: Boolean) {
        view?.post { publishMenuItem?.isVisible = show }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                    getString(R.string.product_update_dialog_title),
                    getString(R.string.product_update_dialog_message)
            ).also { it.show(requireFragmentManager(), CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun getFragmentTitle() = productTitle

    private fun showProduct(productData: ViewState) {
        if (!isAdded) return

        val product = requireNotNull(productData.product)
        val productName = HtmlUtils.fastStripHtml(product.name)
        productTitle = when (product.type) {
            EXTERNAL -> getString(R.string.product_name_external, productName)
            GROUPED -> getString(R.string.product_name_grouped, productName)
            VARIABLE -> getString(R.string.product_name_variable, productName)
            else -> {
                if (product.isVirtual) {
                    getString(R.string.product_name_virtual, productName)
                } else {
                    productName
                }
            }
        }

        updateActivityTitle()

        if (product.images.isEmpty() && !viewModel.isUploading()) {
            imageGallery.visibility = View.GONE
            if (FeatureFlag.PRODUCT_IMAGE_CHOOSER.isEnabled(requireActivity())) {
                addImageContainer.visibility = View.VISIBLE
                addImageContainer.setOnClickListener {
                    viewModel.onAddImageClicked()
                }
            }
        } else {
            addImageContainer.visibility = View.GONE
            imageGallery.visibility = View.VISIBLE
            imageGallery.showProductImages(product, this)
        }

        isVariation = product.type == ProductType.VARIATION

        // show status badge for unpublished products
        product.status?.let { status ->
            if (status != ProductStatus.PUBLISH) {
                frameStatusBadge.visibility = View.VISIBLE
                textStatusBadge.text = status.toString(requireActivity())
            }
        }

        addPrimaryCard(productData)

        // display pricing/inventory card only if product is not a variable product
        // since pricing, inventory, shipping and SKU for a variable product can differ per variant
        if (product.type != VARIABLE) {
            if (isAddEditProductRelease1Enabled(product.type)) {
                addSecondaryCard(productData)
            } else {
                addPricingAndInventoryCard(productData)
            }
        }
        addPurchaseDetailsCard(productData)
    }

    private fun addPrimaryCard(productData: ViewState) {
        val product = requireNotNull(productData.product)

        if (isAddEditProductRelease1Enabled(product.type)) {
            addEditableView(DetailCard.Primary, R.string.product_detail_title_hint, productTitle)?.also { view ->
                view.setOnTextChangedListener { viewModel.updateProductDraft(title = it.toString()) }
            }
        } else {
            addPropertyView(DetailCard.Primary, R.string.product_name, productTitle, LinearLayout.VERTICAL)
        }

        if (isAddEditProductRelease1Enabled(product.type)) {
            val productDescription = product.description
            val showCaption = productDescription.isNotEmpty()
            val description = if (productDescription.isEmpty()) {
                getString(R.string.product_description_empty)
            } else {
                productDescription
            }
            addPropertyView(
                    DetailCard.Primary,
                    getString(R.string.product_description),
                    SpannableString(HtmlUtils.fromHtml(description)),
                    LinearLayout.VERTICAL
            )?.also {
                it.showPropertyName(showCaption)
                it.setMaxLines(1)
                it.setClickListener {
                    AnalyticsTracker.track(Stat.PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED)
                    showProductDescriptionEditor(productDescription)
                }
            }
        }

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

        // show product variants only if product type is variable and if there are variations for the product
        if (product.type == VARIABLE &&
                FeatureFlag.PRODUCT_RELEASE_TEASER.isEnabled(context) &&
                product.numVariations > 0) {
            val properties = mutableMapOf<String, String>()
            for (attribute in product.attributes) {
                properties[attribute.name] = attribute.options.size.toString()
            }

            val propertyValue = getPropertyValue(properties, R.string.product_property_variant_formatter)
            addPropertyView(
                    DetailCard.Primary,
                    getString(R.string.product_variations),
                    propertyValue,
                    LinearLayout.VERTICAL
            )?.setClickListener {
                AnalyticsTracker.track(Stat.PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED)
                showProductVariations(product.remoteId)
            }
        } else {
            removePropertyView(DetailCard.Primary, getString(R.string.product_variations))
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

    /**
     * New product detail card UI slated for new products release 1
     */
    private fun addSecondaryCard(productData: ViewState) {
        val product = requireNotNull(productData.product)

        // If we have pricing info, show price & sales price as a group,
        // otherwise provide option to add pricing info for the product
        val hasPricingInfo = product.price != null || product.salePrice != null || product.taxClass.isNotEmpty()
        val pricingGroup = mutableMapOf<String, String>()
        if (hasPricingInfo) {
            // regular product price
            pricingGroup[getString(R.string.product_regular_price)] =
                    requireNotNull(productData.regularPriceWithCurrency)
            // display product sale price, if available
            if (product.salePrice != null) {
                pricingGroup[getString(R.string.product_sale_price)] = requireNotNull(productData.salePriceWithCurrency)
            }

            // display product sale dates, if available
            var dateOnSaleFrom = product.dateOnSaleFrom
            val dateOnSaleTo = product.dateOnSaleTo
            if (dateOnSaleTo != null && dateOnSaleFrom == null) {
                dateOnSaleFrom = Date()
            }
            val saleDates = when {
                (dateOnSaleFrom != null && dateOnSaleTo != null) -> {
                    getProductSaleDates(dateOnSaleFrom, dateOnSaleTo)
                }
                (dateOnSaleFrom != null && dateOnSaleTo == null) -> {
                    getString(R.string.product_sale_date_from, dateOnSaleFrom.formatToMMMddYYYY())
                }
                else -> null
            }
            saleDates?.let {
                pricingGroup[getString(R.string.product_sale_dates)] = it
            }
        } else {
            pricingGroup[""] = getString(R.string.product_price_empty)
        }

        addPropertyGroup(
                DetailCard.Secondary,
                R.string.product_price,
                pricingGroup,
                groupIconId = R.drawable.ic_gridicons_money
        )?.also {
            // display pricing caption only if pricing info is available
            if (!hasPricingInfo) {
                it.showPropertyName(false)
            }
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        val inventoryGroup = when {
            product.manageStock -> mapOf(
                    Pair(getString(R.string.product_stock_status), stockStatusToDisplayString(product.stockStatus)),
                    Pair(getString(R.string.product_backorders), backordersToDisplayString(product.backorderStatus)),
                    Pair(getString(R.string.product_stock_quantity), StringUtils.formatCount(product.stockQuantity)),
                    Pair(getString(R.string.product_sku), product.sku)
            )
            product.sku.isNotEmpty() -> mapOf(Pair(getString(R.string.product_sku), product.sku))
            else -> mapOf(Pair("", getString(R.string.product_inventory_empty)))
        }

        addPropertyGroup(
                DetailCard.Secondary,
                R.string.product_inventory,
                inventoryGroup,
                groupIconId = R.drawable.ic_gridicons_list_checkmark
        )?.also {
            // display inventory caption only if manage stock is true or if product sku is available
            if (!product.manageStock && product.sku.isEmpty()) {
                it.showPropertyName(false)
            }
            it.setClickListener {
                // TODO: add event listener for click
                showProductInventory(product.remoteId)
            }
        }

        val shippingGroup = mapOf(
                Pair(getString(R.string.product_weight), requireNotNull(productData.weightWithUnits)),
                Pair(getString(R.string.product_dimensions), requireNotNull(productData.sizeWithUnits)),
                Pair(getString(R.string.product_shipping_class), product.shippingClass)
        )
        addPropertyGroup(
                DetailCard.Secondary,
                R.string.product_shipping,
                shippingGroup,
                groupIconId = R.drawable.ic_gridicons_shipping
        )?.also {
            it.setClickListener {
                // TODO add event tracking for click
                showProductShipping(product.remoteId)
            }
        }
    }

    /**
     * Existing product detail card UI which that will be replaced by the new design once
     * Product Release 1 changes are completed.
     */
    private fun addPricingAndInventoryCard(productData: ViewState) {
        val product = requireNotNull(productData.product)

        // if we have pricing info this card is "Pricing and inventory" otherwise it's just "Inventory"
        val hasPricingInfo = product.price != null || product.salePrice != null || product.taxClass.isNotEmpty()
        val pricingCard = if (hasPricingInfo) DetailCard.PricingAndInventory else DetailCard.Inventory

        if (hasPricingInfo) {
            // when there's a sale price show price & sales price as a group, otherwise show price separately
            if (product.salePrice != null) {
                val group = mapOf(getString(R.string.product_regular_price)
                        to requireNotNull(productData.regularPriceWithCurrency),
                        getString(R.string.product_sale_price) to requireNotNull(productData.salePriceWithCurrency)
                )
                addPropertyGroup(pricingCard, R.string.product_price, group)
            } else {
                addPropertyView(
                        pricingCard,
                        R.string.product_price,
                        requireNotNull(productData.priceWithCurrency),
                        LinearLayout.VERTICAL
                )
            }
        }

        // show stock properties as a group if stock management is enabled, otherwise show sku separately
        if (product.manageStock) {
            val group = mapOf(
                    Pair(getString(R.string.product_stock_status), stockStatusToDisplayString(product.stockStatus)),
                    Pair(getString(R.string.product_backorders), backordersToDisplayString(product.backorderStatus)),
                    Pair(getString(R.string.product_stock_quantity), StringUtils.formatCount(product.stockQuantity)),
                    Pair(getString(R.string.product_sku), product.sku)
            )
            addPropertyGroup(pricingCard, R.string.product_inventory, group)
        } else {
            addPropertyView(pricingCard, R.string.product_sku, product.sku, LinearLayout.VERTICAL)
        }
    }

    private fun addPurchaseDetailsCard(productData: ViewState) {
        val product = requireNotNull(productData.product)

        // shipping group is part of the secondary card if edit product is enabled
        if (!isAddEditProductRelease1Enabled(product.type)) {
            val shippingGroup = mapOf(
                    Pair(getString(R.string.product_weight), requireNotNull(productData.weightWithUnits)),
                    Pair(getString(R.string.product_size), requireNotNull(productData.sizeWithUnits)),
                    Pair(getString(R.string.product_shipping_class), product.shippingClass)
            )
            addPropertyGroup(DetailCard.PurchaseDetails, R.string.product_shipping, shippingGroup)
        }

        if (product.isDownloadable) {
            val limit = if (product.downloadLimit > 0) String.format(
                    getString(R.string.product_download_limit_count),
                    product.downloadLimit
            ) else ""
            val expiry = if (product.downloadExpiry > 0) String.format(
                    getString(R.string.product_download_expiry_days),
                    product.downloadExpiry
            ) else ""

            val downloadGroup = mapOf(
                    Pair(getString(R.string.product_downloadable_files), product.fileCount.toString()),
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
        orientation: Int = LinearLayout.HORIZONTAL,
        @DrawableRes propertyIcon: Int? = null
    ): WCProductPropertyView? {
        return addPropertyView(card, propertyName, SpannableString(propertyValue), orientation, propertyIcon)
    }

    private fun removePropertyView(
        card: DetailCard,
        propertyName: String
    ) {
        // locate the card, add it if it doesn't exist yet
        val cardView = findOrAddCardView(card)

        // locate the linear layout container inside the card
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)

        // locate the existing property view in the container, add it if not found
        val propertyTag = "{$propertyName}_tag"
        val propertyView = container.findViewWithTag<WCProductPropertyView>(propertyTag)
        if (propertyView != null) {
            container.removeView(propertyView)
        }
    }

    private fun addPropertyView(
        card: DetailCard,
        propertyName: String,
        propertyValue: SpannableString,
        orientation: Int = LinearLayout.HORIZONTAL,
        @DrawableRes propertyIcon: Int? = null
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
            propertyView = WCProductPropertyView(requireActivity())
            propertyView.tag = propertyTag
            container.addView(propertyView)
        }

        propertyView.show(orientation, propertyName, propertyValue, propertyIcon)
        return propertyView
    }

    /**
     * Adds a group of related properties as a single property view
     */
    private fun addPropertyGroup(
        card: DetailCard,
        @StringRes groupTitleId: Int,
        properties: Map<String, String>,
        @StringRes propertyValueFormatterId: Int = R.string.product_property_default_formatter,
        propertyGroupClickListener: ((view: View) -> Unit)? = null,
        @DrawableRes groupIconId: Int? = null
    ): WCProductPropertyView? {
        val propertyValue = getPropertyValue(properties, propertyValueFormatterId)
        return addPropertyView(card, getString(groupTitleId), propertyValue, LinearLayout.VERTICAL, groupIconId)?.also {
            it.setClickListener(propertyGroupClickListener)
        }
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
            linkView = WCProductPropertyLinkView(requireActivity())
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
            readMoreView = WCProductPropertyReadMoreView(requireActivity())
            readMoreView.tag = readMoreTag
            container.addView(readMoreView)
        }

        readMoreView.show(caption, HtmlUtils.fastStripHtml(content), maxLines)
    }

    /**
     * Adds an editText to the passed card
     */
    private fun addEditableView(
        card: DetailCard,
        @StringRes propertyNameId: Int,
        propertyValue: String?
    ): WCProductPropertyEditableView? {
        val hint = getString(propertyNameId)
        val editableViewTag = "${hint}_tag"

        val cardView = findOrAddCardView(card)
        val container = cardView.findViewById<LinearLayout>(R.id.cardContainerView)
        var editableView = container.findViewWithTag<WCProductPropertyEditableView>(editableViewTag)

        if (editableView == null) {
            editableView = WCProductPropertyEditableView(requireActivity())
            editableView.tag = editableViewTag
            container.addView(editableView)
        }

        editableView.show(hint, propertyValue)
        return editableView
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
            addCardDividerView(requireActivity())
        }

        val cardView = WCProductPropertyCardView(requireActivity())
        cardView.tag = cardTag

        val cardViewCaption: String? = when (card) {
            DetailCard.Primary -> null
            DetailCard.Secondary -> null
            DetailCard.PricingAndInventory -> getString(R.string.product_pricing_and_inventory)
            DetailCard.Inventory -> getString(R.string.product_inventory)
            DetailCard.PurchaseDetails -> getString(R.string.product_purchase_details)
        }

        cardView.show(cardViewCaption)
        productDetail_container.addView(cardView)

        return cardView
    }

    /**
     * Given a map of product properties [properties] and a formatter [propertyValueFormatterId]
     * returns a String with the property names and corresponding values
     * Eg:
     * Regular Price: $20.00
     * Sale Price: $10.00
     *      OR
     * Color: 3 options
     * Size: 2 options
     */
    private fun getPropertyValue(
        properties: Map<String, String>,
        @StringRes propertyValueFormatterId: Int = R.string.product_property_default_formatter
    ): String {
        var propertyValue = ""
        properties.forEach { property ->
            if (property.key.isEmpty()) {
                propertyValue += property.value
            } else if (property.value.isNotEmpty()) {
                if (propertyValue.isNotEmpty()) {
                    propertyValue += "\n"
                }
                propertyValue += getString(propertyValueFormatterId, property.key, property.value)
            }
        }
        return propertyValue
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

    private fun shareProduct(product: Product) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, productTitle)
            putExtra(Intent.EXTRA_TEXT, product.permalink)
            type = "text/plain"
        }
        val title = resources.getText(R.string.product_share_dialog_title)
        startActivity(Intent.createChooser(shareIntent, title))
    }

    private fun showProductVariations(remoteId: Long) {
        val action = ProductDetailFragmentDirections
                .actionProductDetailFragmentToProductVariantsFragment(remoteId)
        findNavController().navigate(action)
    }

    private fun showProductDescriptionEditor(productDescription: String) {
        findNavController().navigate(ProductDetailFragmentDirections
                .actionProductDetailFragmentToAztecEditorFragment(
                        productDescription, getString(R.string.product_description
                )))
    }

    private fun showProductInventory(remoteId: Long) {
        findNavController().navigate(
                ProductDetailFragmentDirections
                        .actionProductDetailFragmentToProductInventoryFragment(remoteId)
        )
    }

    private fun showProductShipping(remoteId: Long) {
        findNavController().navigate(
                ProductDetailFragmentDirections
                        .actionProductDetailFragmentToProductShippingFragment(remoteId)
        )
    }

    private fun getProductSaleDates(dateOnSaleFrom: Date, dateOnSaleTo: Date): String {
        val formattedFromDate = if (DateTimeUtils.isSameYear(dateOnSaleFrom, dateOnSaleTo)) {
            dateOnSaleFrom.formatToMMMdd()
        } else {
            dateOnSaleFrom.formatToMMMddYYYY()
        }
        return getString(R.string.product_sale_date_from_to, formattedFromDate, dateOnSaleTo.formatToMMMddYYYY())
    }

    /**
     * returns the product's stock status formatted for display
     */
    private fun stockStatusToDisplayString(status: ProductStockStatus): String {
        return if (status.stringResource != 0) {
            getString(status.stringResource)
        } else {
            status.value
        }
    }

    private fun backordersToDisplayString(status: ProductBackorderStatus): String {
        return if (status.stringResource != 0) {
            getString(status.stringResource)
        } else {
            status.value
        }
    }

    override fun onNavigationResult(requestCode: Int, result: Bundle) {
        when (requestCode) {
            AZTEC_EDITOR_REQUEST_CODE -> {
                if (result.getBoolean(AztecEditorFragment.ARG_AZTEC_HAS_CHANGES)) {
                    viewModel.updateProductDraft(result.getString(ARG_AZTEC_EDITOR_TEXT))
                }
            }
        }
    }

    override fun onGalleryImageClicked(image: Product.Image, imageView: View) {
        clickedImage = WeakReference(imageView)
        viewModel.onImageGalleryClicked(image)
    }

    override fun onGalleryAddImageClicked() {
        showImageChooser()
    }

    private fun showProductImages(product: Product, imageModel: Product.Image? = null) {
        if (FeatureFlag.PRODUCT_IMAGE_CHOOSER.isEnabled(requireActivity())) {
            showImageChooser()
        } else if (imageModel != null) {
            ImageViewerActivity.showProductImages(
                    this,
                    product,
                    imageModel,
                    sharedElement = clickedImage.get()
            )
        }
    }

    private fun showImageChooser() {
        val action = ProductDetailFragmentDirections
                .actionProductDetailFragmentToProductImagesFragment(navArgs.remoteProductId)
        findNavController().navigate(action)
    }

    /**
     * Add/Edit Product Release 1 is enabled only for debug users for SIMPLE products only
     */
    private fun isAddEditProductRelease1Enabled(productType: ProductType) =
            FeatureFlag.ADD_EDIT_PRODUCT_RELEASE_1.isEnabled() && productType == ProductType.SIMPLE
}
