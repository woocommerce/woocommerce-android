package com.woocommerce.android.ui.products

import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        product.getFirstImage()?.let {
            val imageWidth = DisplayUtils.getDisplayPixelWidth(activity)
            val imageHeight = resources.getDimensionPixelSize(R.dimen.product_image_height)
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageWidth, imageHeight)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .error(R.drawable.ic_product)
                    .into(productDetail_image)
        }

        addView(R.string.product_name, product.name)
        addView(R.string.product_description, product.description)
        addView(R.string.product_short_description, product.shortDescription)
        addView(R.string.product_purchase_note, product.purchaseNote)
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

    private fun addView(@StringRes captionId: Int, detail: String, showDivider: Boolean = true): WCCaptionedTextView? {
        if (detail.isBlank() || view == null) return null

        // find the existing view, if not found then add it
        var captionedView = view!!.findViewWithTag<WCCaptionedTextView>(captionId.toString())
        if (captionedView == null) {
            captionedView = WCCaptionedTextView(activity as Context)
            productDetail_container.addView(captionedView)
        }

        // some details, such as product description, contain html which needs to be stripped here
        val detailPlainText = HtmlUtils.fastStripHtml(detail).trim()
        captionedView.show(getString(captionId), detailPlainText, showDivider)

        // tag the view so we can use findViewWithTag above
        captionedView.tag = captionId.toString()

        return captionedView
    }
}
