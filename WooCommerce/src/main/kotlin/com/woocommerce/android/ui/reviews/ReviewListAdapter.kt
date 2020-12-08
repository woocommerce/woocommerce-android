package com.woocommerce.android.ui.reviews

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
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
import com.woocommerce.android.widgets.sectionedrecyclerview.Section
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.sectionedrecyclerview.StatelessSection

class ReviewListAdapter(
    private val context: Context,
    private val clickListener: OnReviewClickListener
) : SectionedRecyclerViewAdapter() {
    private val reviewList = mutableListOf<ProductReview>()

    // Copy of current review manually removed from the list so the action may be undone.
    private var pendingRemovalReview: Triple<ProductReview, ReviewListSection, Int>? = null

    // List of all remote note IDs the user has removed this session
    private val removedRemoteIds = HashSet<Long>()

    interface OnReviewClickListener {
        fun onReviewClick(review: ProductReview) { }
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

        // remove any items temporarily being hidden
        pendingRemovalReview?.first?.remoteId?.let { hideReviewWithId(it, notifyDataChanged = false) }

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
                        it.status == review.status) {
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
     * Locates and removes the review from the appropriate section, but keeps a reference to
     * it so it may be be restored if needed. This temporary object will get cleared either manually
     * by reverting the action, or by loading a fresh list of product reviews.
     *
     * If we've hidden this item already, but have since received fresh data, set [notifyDataChanged] as false to
     * suppress the dataChanged notifications and prevent the UI from jumping.
     *
     * @param remoteId The remote Id of the product review
     * @param notifyDataChanged If true, notify the UI of changes to the underlying data set. Default true.
     */
    fun hideReviewWithId(remoteId: Long, notifyDataChanged: Boolean = true) {
        val posInList = reviewList.indexOfFirst { it.remoteId == remoteId }
        if (posInList == -1) {
            WooLog.w(T.REVIEWS, "Unable to hide product review, position is -1")
            pendingRemovalReview = null
            removedRemoteIds.remove(remoteId)
            return
        }

        getSectionForListItemPosition(posInList)?.let {
            val section = it as ReviewListSection
            val posInSection = getPositionInSectionByListPos(posInList)
            pendingRemovalReview = Triple(reviewList[posInList], section, posInSection)

            // remove from the section list
            section.list.removeAt(posInSection)

            if (!removedRemoteIds.contains(remoteId)) {
                removedRemoteIds.add(remoteId)
            }

            if (notifyDataChanged) {
                notifyItemRemovedFromSection(section, posInSection)
            }

            if (section.list.size == 0) {
                val sectionPos = getSectionPosition(section)
                section.isVisible = false
                if (sectionPos != INVALID_POSITION && notifyDataChanged) {
                    notifySectionChangedToInvisible(section, sectionPos)
                }
            }
        }
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
                return if (review.read ?: true) ItemType.READ else ItemType.UNREAD
            }
            currentPos++
        }

        WooLog.w(T.REVIEWS, "Failed to get item type at review recycler position $position")
        return ItemType.READ
    }

    /**
     * Inserts the previously removed review and notifies the recycler view.
     * @return The position in the adapter the item was added to
     */
    fun revertHiddenReviewAndReturnPos(): Int {
        return pendingRemovalReview?.let { (review, section, pos) ->
            if (!section.isVisible) {
                section.isVisible = true
                notifySectionChangedToVisible(section)
            }

            with(section.list) {
                if (pos < size) {
                    add(pos, review)
                } else {
                    add(review)
                }
            }

            removedRemoteIds.remove(review.remoteId)
            pendingRemovalReview = null

            notifyItemInsertedInSection(section, pos)
            getPositionInAdapter(section, pos)
        } ?: INVALID_POSITION
    }

    /**
     * Removes the previously hidden review from the main list so changes from the
     * database will be properly applied.
     */
    fun removeHiddenReviewFromList() {
        pendingRemovalReview?.let { (review, _, _) ->
            reviewList.remove(review)

            removedRemoteIds.remove(review.remoteId)
            pendingRemovalReview = null
        }
    }

    /**
     * Resets any pending review moderation state
     */
    fun resetPendingModerationState() {
        pendingRemovalReview = null
    }

    fun isEmpty() = reviewList.isEmpty()
    // endregion

    // region Private methods
    /**
     * Return the item position relative to the section.
     *
     * @param position position of the item in the original backing list
     * @return position of the item in the section
     */
    private fun getPositionInSectionByListPos(position: Int): Int {
        var currentPos = 0

        sectionsMap.entries.forEach {
            val section = it.value
            val sectionTotal = section.getContentItemsTotal()

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                return position - currentPos
            }

            currentPos += sectionTotal
        }

        // position not found, fail fast
        throw IndexOutOfBoundsException("Unable to find matching position $position in section")
    }

    /**
     * Returns the Section object for a position in the backing list.
     *
     * @param position position in the original list
     * @return Section object for that position or null if not found
     */
    private fun getSectionForListItemPosition(position: Int): Section? {
        var currentPos = 0

        sectionsMap.entries.forEach {
            val section = it.value
            val sectionTotal = section.getContentItemsTotal()

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                return section
            }

            currentPos += sectionTotal
        }

        // position not found, fail fast
        WooLog.w(T.REVIEWS, "Unable to find matching section for position $position")
        return null
    }
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
            itemHolder.bind(review, position, getContentItemsTotal())
            itemHolder.itemView.setOnClickListener {
                clickListener.onReviewClick(review)
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
        fun bind(review: ProductReview, position: Int, totalItems: Int) {
            viewBinding.notifRating.visibility = View.GONE
            viewBinding.notifIcon.setImageResource(R.drawable.ic_comment)
            viewBinding.notifDesc.maxLines = 2

            if (review.rating > 0) {
                viewBinding.notifRating.numStars = review.rating
                viewBinding.notifRating.rating = 100F // necessary to hide unfilled stars
                viewBinding.notifRating.visibility = View.VISIBLE
            } else {
                viewBinding.notifRating.visibility = View.GONE
            }

            viewBinding.notifTitle.text = if (review.product == null) {
                viewBinding.root.context.getString(
                    R.string.product_review_list_item_title, review.reviewerName)
            } else {
                viewBinding.root.context.getString(
                    R.string.review_list_item_title, review.reviewerName, review.product?.name?.fastStripHtml())
            }

            viewBinding.notifDesc.text = StringUtils.getRawTextFromHtml(review.review)

            if (position == totalItems - 1) {
                viewBinding.notifDivider.visibility = View.INVISIBLE
            }
        }
    }

    private class HeaderViewHolder(val viewBinding: OrderListHeaderBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(@StringRes headerId: Int) {
            viewBinding.orderListHeader.text = viewBinding.root.context.getString(headerId)
        }
    }
}
