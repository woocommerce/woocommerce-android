package com.woocommerce.android.ui.reviews

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.NotifsListItemBinding
import com.woocommerce.android.databinding.OrderListHeaderBinding
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.UnreadItemDecoration.ItemType
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.sectionedrecyclerview.StatelessSection

class ReviewListAdapter(private val clickListener: OnReviewClickListener) : SectionedRecyclerViewAdapter() {
    private val reviewList = mutableListOf<ProductReview>()

    interface OnReviewClickListener {
        fun onReviewClick(review: ProductReview, sharedView: View? = null) {}
    }

    fun setReviews(reviews: List<ProductReview>) {
        if (isSameList(reviews)) {
            // No changes to display, exit.
            return
        }

        // clear all the current data from the adapter
        removeAllSections()

        // Build a reviews for each [TimeGroup] section
        val listFuture = ArrayList<ProductReview>() // Should never be needed, but some extension could change that
        val listToday = ArrayList<ProductReview>()
        val listYesterday = ArrayList<ProductReview>()
        val listTwoDays = ArrayList<ProductReview>()
        val listWeek = ArrayList<ProductReview>()
        val listMonth = ArrayList<ProductReview>()

        reviews.forEach {
            // Default to today if the date cannot be parsed
            when (TimeGroup.getTimeGroupForDate(it.dateCreated)) {
                TimeGroup.GROUP_TODAY -> listToday.add(it)
                TimeGroup.GROUP_YESTERDAY -> listYesterday.add(it)
                TimeGroup.GROUP_OLDER_TWO_DAYS -> listTwoDays.add(it)
                TimeGroup.GROUP_OLDER_WEEK -> listWeek.add(it)
                TimeGroup.GROUP_OLDER_MONTH -> listMonth.add(it)
                TimeGroup.GROUP_FUTURE -> listFuture.add(it)
            }
        }

        if (listFuture.size > 0) {
            addSection(ReviewListSection(TimeGroup.GROUP_FUTURE.name, listFuture))
        }

        if (listToday.size > 0) {
            addSection(ReviewListSection(TimeGroup.GROUP_TODAY.name, listToday))
        }

        if (listYesterday.size > 0) {
            addSection(ReviewListSection(TimeGroup.GROUP_YESTERDAY.name, listYesterday))
        }

        if (listTwoDays.size > 0) {
            addSection(ReviewListSection(TimeGroup.GROUP_OLDER_TWO_DAYS.name, listTwoDays))
        }

        if (listWeek.size > 0) {
            addSection(ReviewListSection(TimeGroup.GROUP_OLDER_WEEK.name, listWeek))
        }

        if (listMonth.size > 0) {
            addSection(ReviewListSection(TimeGroup.GROUP_OLDER_MONTH.name, listMonth))
        }

        // remember these reviews for comparison in isSameList() below
        reviewList.clear()
        reviewList.addAll(reviews)

        notifyDataSetChanged()
    }

    private fun isSameList(reviews: List<ProductReview>): Boolean {
        if (reviews.size != reviewList.size) {
            return false
        }

        val didMatch = fun(review: ProductReview): Boolean {
            reviewList.forEach {
                if (it.remoteId == review.remoteId &&
                    it.review == review.review &&
                    it.read == (review.read != false) &&
                    it.status == review.status
                ) {
                    return true
                }
            }
            return false
        }

        reviews.forEach {
            if (!didMatch(it)) {
                return false
            }
        }

        return true
    }

    /**
     * Return true if the item at the passed position is a header
     *
     * @param position position of the item in the recycler
     */
    private fun isHeaderAtRecyclerPosition(position: Int): Boolean {
        var currentPos = 0
        val sections = sectionsMap

        for ((_, section) in sections) {
            val sectionTotal = section.sectionItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                if (section.hasHeader() && position == currentPos) {
                    return true
                }
            }

            currentPos += sectionTotal
        }

        return false
    }

    /**
     * Returns the type of item at the passed position for use so the item decoration
     * can draw a bar beside unread items
     *
     * @param position position of the item in the recycler
     */
    fun getItemTypeAtRecyclerPosition(position: Int): ItemType {
        if (isHeaderAtRecyclerPosition(position)) {
            return ItemType.HEADER
        }

        var currentPos = 0
        for (review in reviewList) {
            if (isHeaderAtRecyclerPosition(currentPos)) {
                currentPos++
            }
            if (currentPos == position) {
                return if (review.read != false) ItemType.READ else ItemType.UNREAD
            }
            currentPos++
        }

        WooLog.w(T.REVIEWS, "Failed to get item type at review recycler position $position")
        return ItemType.READ
    }

    fun isEmpty() = reviewList.isEmpty()
    // endregion

    private inner class ReviewListSection(
        val title: String,
        val list: MutableList<ProductReview>
    ) : StatelessSection(
        SectionParameters.Builder(R.layout.notifs_list_item).headerResourceId(R.layout.order_list_header).build()
    ) {
        override fun getContentItemsTotal() = list.size

        override fun getItemViewHolder(view: View): ItemViewHolder {
            val parent = view as ViewGroup
            return ItemViewHolder(
                NotifsListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindItemViewHolder(holder: ViewHolder, position: Int) {
            val review = list[position]
            val itemHolder = holder as ItemViewHolder
            itemHolder.bind(
                review,
                position,
                getContentItemsTotal(),
                reviewStatus = ProductReviewStatus.fromString(review.status)
            )
            itemHolder.itemView.setOnClickListener {
                clickListener.onReviewClick(review, itemHolder.itemView)
            }
        }

        override fun getHeaderViewHolder(view: View): HeaderViewHolder {
            val parent = view as ViewGroup
            return HeaderViewHolder(
                OrderListHeaderBinding.inflate(
                    LayoutInflater.from(view.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindHeaderViewHolder(holder: ViewHolder) {
            @StringRes val headerId = when (TimeGroup.valueOf(title)) {
                TimeGroup.GROUP_OLDER_MONTH -> R.string.date_timeframe_older_month
                TimeGroup.GROUP_OLDER_WEEK -> R.string.date_timeframe_older_week
                TimeGroup.GROUP_OLDER_TWO_DAYS -> R.string.date_timeframe_older_two_days
                TimeGroup.GROUP_YESTERDAY -> R.string.date_timeframe_yesterday
                TimeGroup.GROUP_TODAY -> R.string.date_timeframe_today
                TimeGroup.GROUP_FUTURE -> R.string.date_timeframe_future
            }

            (holder as HeaderViewHolder).bind(headerId)
        }
    }

    private class ItemViewHolder(val viewBinding: NotifsListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        private val context = viewBinding.root.context
        private val bullet = "\u2022"
        private val pendingLabelColor: Int = ContextCompat.getColor(context, R.color.woo_orange_50)
        private val notifsIconPendingColor: Int = ContextCompat.getColor(context, R.color.woo_purple_60)

        fun bind(review: ProductReview, position: Int, totalItems: Int, reviewStatus: ProductReviewStatus) {
            viewBinding.notifRating.visibility = View.GONE
            viewBinding.notifIcon.setImageResource(R.drawable.ic_comment)
            viewBinding.notifDesc.maxLines = 2

            ViewCompat.setTransitionName(
                viewBinding.root,
                context.getString(
                    R.string.review_card_transition_name,
                    review.remoteId.toString()
                )
            )

            if (review.rating > 0) {
                viewBinding.notifRating.numStars = review.rating
                viewBinding.notifRating.rating = 100F // necessary to hide unfilled stars
                viewBinding.notifRating.visibility = View.VISIBLE
            } else {
                viewBinding.notifRating.visibility = View.GONE
            }

            viewBinding.notifTitle.text = if (review.product == null) {
                viewBinding.root.context.getString(
                    R.string.product_review_list_item_title, review.reviewerName
                )
            } else {
                viewBinding.root.context.getString(
                    R.string.review_list_item_title, review.reviewerName, review.product?.name?.fastStripHtml()
                )
            }

            val reviewText: String = StringUtils.getRawTextFromHtml(review.review)

            if (reviewStatus == ProductReviewStatus.HOLD) {
                val pendingReviewText = getPendingReviewLabel()
                viewBinding.notifDesc.text = HtmlCompat.fromHtml(
                    "$pendingReviewText $bullet $reviewText",
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
                viewBinding.notifIcon.setColorFilter(notifsIconPendingColor)
            } else {
                viewBinding.notifIcon.colorFilter = null
                viewBinding.notifDesc.text = reviewText
            }
            if (position == totalItems - 1) {
                viewBinding.notifDivider.visibility = View.INVISIBLE
            }
        }

        private fun getPendingReviewLabel() =
            "<font color=$pendingLabelColor>${context.getString(R.string.pending_review_label)}</font>"
    }
}

private class HeaderViewHolder(val viewBinding: OrderListHeaderBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {
    fun bind(@StringRes headerId: Int) {
        viewBinding.orderListHeader.text = viewBinding.root.context.getString(headerId)
    }
}
