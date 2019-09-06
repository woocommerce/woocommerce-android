package com.woocommerce.android.ui.reviews

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_review_detail.*
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class ReviewDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap

    private lateinit var viewModel: ReviewDetailViewModel

    private var runOnStartFunc: (() -> Unit)? = null
    private var productIconSize: Int = 0
    private val skeletonView = SkeletonView()

    private val navArgs: ReviewDetailFragmentArgs by navArgs()

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dimen = activity!!.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
        productIconSize = DisplayUtils.dpToPx(activity, dimen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    override fun onStart() {
        super.onStart()

        runOnStartFunc?.let {
            it.invoke()
            runOnStartFunc = null
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun getFragmentTitle() = getString(R.string.wc_review_title)

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ReviewDetailViewModel::class.java).also {
            setupObservers(it)
        }

        viewModel.start(navArgs.remoteReviewId)
    }

    private fun setupObservers(viewModel: ReviewDetailViewModel) {
        viewModel.productReview.observe(this, Observer {
            setReview(it)
        })

        viewModel.isSkeletonShown.observe(this, Observer {
            showSkeleton(it)
        })

        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
        })

        viewModel.refreshProductImage.observe(this, Observer {
            refreshProductImage(it)
        })

        viewModel.exit.observe(this, Observer {
            exitDetailView()
        })

        viewModel.markAsRead.observe(this, Observer { remoteNoteId ->
            // Remove all active notifications from the system bar
            context?.let {
                NotificationHandler.removeNotificationWithNoteIdFromSystemBar(it, remoteNoteId.toString())
            }
        })
    }

    private fun setReview(review: ProductReview) {
        // adjust the gravatar url so it's requested at the desired size and a has default image of 404 (this causes the
        // request to return a 404 rather than an actual default image URL, so we can stick with our default avatar)
        val size = activity?.resources?.getDimensionPixelSize(R.dimen.avatar_sz_large) ?: 256
        val avatarUrl = UrlUtils.removeQuery(review.reviewerAvatarUrl) + "?s=" + size + "&d=404"

        // Populate reviewer section
        GlideApp.with(review_gravatar.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_user_circle_grey_24dp)
                .circleCrop()
                .into(review_gravatar)
        review_user_name.text = review.reviewerName
        review_time.text = DateTimeUtils.timeSpanFromTimestamp(review.dateCreated.time, activity as Context)

        // Populate reviewed product info
        review.product?.let { product ->
            review_product_name.text = product.name
            review_open_product.setOnClickListener {
                AnalyticsTracker.track(Stat.REVIEW_DETAIL_OPEN_EXTERNAL_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(activity as Context, product.externalUrl)
            }
            refreshProductImage(product.remoteProductId)
        }

        review_rating_bar.rating = review.rating.toFloat()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val stars = review_rating_bar.progressDrawable as? LayerDrawable
            stars?.getDrawable(2)?.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.alert_yellow),
                    PorterDuff.Mode.SRC_ATOP
            )
        }

        // Set the review text
        review_description.text = HtmlUtils.fromHtml(review.review)
    }

    private fun refreshProductImage(remoteProductId: Long) {
        // Note that if productImageMap doesn't already have the image for this product then it will request
        // it from the backend. When the request completes it will be captured by the presenter, which will
        // call this method to show the image for the just-downloaded product model
        productImageMap.get(remoteProductId)?.let { productImage ->
            val imageUrl = PhotonUtils.getPhotonImageUrl(productImage, productIconSize, productIconSize)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(review_product_icon)
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(container, R.layout.skeleton_notif_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun exitDetailView() {
        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
        }
    }
}
