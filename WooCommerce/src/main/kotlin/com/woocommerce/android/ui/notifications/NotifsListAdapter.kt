package com.woocommerce.android.ui.notifications

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.extensions.WooNotificationType.NEW_ORDER
import com.woocommerce.android.extensions.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.extensions.WooNotificationType.UNKNOWN
import com.woocommerce.android.extensions.getMessageSnippet
import com.woocommerce.android.extensions.getRating
import com.woocommerce.android.extensions.getTitleSnippet
import com.woocommerce.android.extensions.getWooType
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.util.applyTransform
import com.woocommerce.android.widgets.Section
import com.woocommerce.android.widgets.SectionParameters
import com.woocommerce.android.widgets.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.StatelessSection
import kotlinx.android.synthetic.main.notifs_list_item.view.*
import kotlinx.android.synthetic.main.order_list_header.view.*
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import java.util.Date
import javax.inject.Inject

class NotifsListAdapter @Inject constructor() : SectionedRecyclerViewAdapter() {
    enum class ItemType {
        HEADER,
        UNREAD_NOTIF,
        READ_NOTIF
    }

    interface ItemDecorationListener {
        fun getItemTypeAtPosition(position: Int): ItemType
    }

    interface ReviewListListener {
        fun onNotificationClicked(notification: NotificationModel)
    }

    private val notifsList = mutableListOf<NotificationModel>()
    private var listListener: ReviewListListener? = null

    // Copy of a notification manually removed from the list so the action may be undone.
    private var pendingRemovalNotification: Triple<NotificationModel, NotifsListSection, Int>? = null

    // region Public methods
    fun setListListener(listener: ReviewListListener) {
        listListener = listener
    }

    fun setNotifications(notifs: List<NotificationModel>) {
        // If moderation pending review is present, make sure it's removed
        // before processing the list.
        val newList = pendingRemovalNotification?.let { (notif, _, _) ->
            notifs.toMutableList().also { it.remove(notif) }
        } ?: notifs

        // clear all the current data from the adapter
        removeAllSections()

        // Build a notifs for each [TimeGroup] section
        val listToday = ArrayList<NotificationModel>()
        val listYesterday = ArrayList<NotificationModel>()
        val listTwoDays = ArrayList<NotificationModel>()
        val listWeek = ArrayList<NotificationModel>()
        val listMonth = ArrayList<NotificationModel>()

        newList.forEach {
            // Default to today if the date cannot be parsed
            val date: Date = DateTimeUtils.dateUTCFromIso8601(it.timestamp) ?: Date()
            val timeGroup = TimeGroup.getTimeGroupForDate(date)
            when (timeGroup) {
                TimeGroup.GROUP_TODAY -> listToday.add(it)
                TimeGroup.GROUP_YESTERDAY -> listYesterday.add(it)
                TimeGroup.GROUP_OLDER_TWO_DAYS -> listTwoDays.add(it)
                TimeGroup.GROUP_OLDER_WEEK -> listWeek.add(it)
                TimeGroup.GROUP_OLDER_MONTH -> listMonth.add(it)
            }
        }

        // ******************************************************************************************
        // TODO: remove this test code which alternates the read state in the passed notifs list
        fun testUpdateList(testList: ArrayList<NotificationModel>) {
            for (i in 0 until testList.size) {
                testList[i].read = i % 2 == 0
            }
        }
        testUpdateList(listToday)
        testUpdateList(listYesterday)
        testUpdateList(listTwoDays)
        testUpdateList(listWeek)
        testUpdateList(listMonth)

        // ******************************************************************************************

        if (listToday.size > 0) {
            addSection(NotifsListSection(TimeGroup.GROUP_TODAY.name, listToday))
        }

        if (listYesterday.size > 0) {
            addSection(NotifsListSection(TimeGroup.GROUP_YESTERDAY.name, listYesterday))
        }

        if (listTwoDays.size > 0) {
            addSection(NotifsListSection(TimeGroup.GROUP_OLDER_TWO_DAYS.name, listTwoDays))
        }

        if (listWeek.size > 0) {
            addSection(NotifsListSection(TimeGroup.GROUP_OLDER_WEEK.name, listWeek))
        }

        if (listMonth.size > 0) {
            addSection(NotifsListSection(TimeGroup.GROUP_OLDER_MONTH.name, listMonth))
        }

        notifyDataSetChanged()

        // remember these notifications for comparison in isSameList() below
        notifsList.clear()
        notifsList.addAll(newList)
    }

    fun isSameList(notifs: List<NotificationModel>): Boolean {
        if (notifs.size != notifsList.size) {
            return false
        }

        val didMatch = fun(notification: NotificationModel): Boolean {
            notifsList.forEach {
                if (it.noteId == notification.noteId &&
                        it.title == notification.title &&
                        it.read == notification.read &&
                        it.noteHash == notification.noteHash) {
                    return true
                }
            }
            return false
        }

        notifs.forEach {
            if (!didMatch(it)) {
                return false
            }
        }

        return true
    }

    /**
     * Locates and removes the notification from the appropriate section, but keeps a reference to
     * it so it may be be restored if needed. This temporary object will get cleared either manually
     * by reverting the action, or by loading a fresh list of notifications.
     */
    fun hideNotificationWithId(remoteNoteId: Long) {
        notifsList.firstOrNull { it.remoteNoteId == remoteNoteId }?.let { notif ->
            // get the index
            val pos = notifsList.indexOfFirst { it == notif }

            // remove from the list
            val section = getSectionForListItemPosition(pos) as NotifsListSection
            val posInSection = getPositionInSectionByListPos(pos)
            pendingRemovalNotification = Triple(notif, section, posInSection)

            section.list.removeAt(posInSection)
            notifyItemRemovedFromSection(section, posInSection)

            if (section.list.size == 0) {
                val sectionPos = getSectionPosition(section)
                section.isVisible = false
                notifySectionChangedToInvisible(section, sectionPos)
            }
        }
    }

    /**
     * Return true if the item at the passed position is a header
     *
     * @param position position of the item in the recycler
     */
    private fun isHeaderAtPosition(position: Int): Boolean {
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
     * Returns the type of item at the passed position
     *
     * @param position position of the item in the recycler
     */
    fun getItemTypeAtPosition(position: Int): ItemType {
        if (isHeaderAtPosition(position)) {
            return ItemType.HEADER
        }

        return try {
            val section = getSectionForListItemPosition(position) as NotifsListSection
            val posInSection = getPositionInSectionByListPos(position)
            val notif = section.list[posInSection]
            if (notif.read) {
                ItemType.READ_NOTIF
            } else {
                ItemType.UNREAD_NOTIF
            }
        } catch (e: IndexOutOfBoundsException) {
            WooLog.e(T.NOTIFICATIONS, e)
            ItemType.READ_NOTIF
        }
    }

    /**
     * Inserts the previously removed notification and notifies the recycler view.
     * @return The position in the adapter the item was added to
     */
    fun revertHiddenNotificationAndReturnPos(): Int {
        return pendingRemovalNotification?.let { (notif, section, pos) ->
            with(section) {
                if (pos < list.size) {
                    list.add(pos, notif)
                } else {
                    list.add(notif)
                }
            }

            if (!section.isVisible) {
                section.isVisible = true
                notifySectionChangedToVisible(section)
            }

            notifyItemInsertedInSection(section, pos)
            getPositionInAdapter(section, pos)
        }.also { pendingRemovalNotification = null } ?: 0
    }

    /**
     * Resets any pending review moderation state
     */
    fun resetPendingModerationState() {
        pendingRemovalNotification = null
    }

    /**
     * Superficially marks all notifications in the current list as read by creating a
     * copy of the existing list, then setting the [NotificationModel#read] property to true and
     * feeding the updated list back into the adapter.
     */
    fun markAllNotifsAsRead() {
        val newList = mutableListOf<NotificationModel>()
                .apply { addAll(notifsList) }.applyTransform { it.apply { read = true } }

        setNotifications(newList)
    }
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
            val sectionTotal = section.contentItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                return position - currentPos
            }

            currentPos += sectionTotal
        }

        // position not found, fail fast
        throw IndexOutOfBoundsException("Unable to find matching position in section")
    }

    /**
     * Returns the Section object for a position in the backing list.
     *
     * @param position position in the original list
     * @return Section object for that position
     */
    private fun getSectionForListItemPosition(position: Int): Section {
        var currentPos = 0

        sectionsMap.entries.forEach {
            val section = it.value
            val sectionTotal = section.contentItemsTotal

            // check if position is in this section
            if (position >= currentPos && position <= currentPos + sectionTotal - 1) {
                return section
            }

            currentPos += sectionTotal
        }

        // position not found, fail fast
        throw IndexOutOfBoundsException("Unable to find matching position in section")
    }
    // endregion

    private inner class NotifsListSection(
        val title: String,
        val list: MutableList<NotificationModel>
    ) : StatelessSection(
            SectionParameters.Builder(R.layout.notifs_list_item).headerResourceId(R.layout.order_list_header).build()
    ) {
        override fun getContentItemsTotal() = list.size

        override fun getItemViewHolder(view: View) = ItemViewHolder(view)

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val notif = list[position]
            val itemHolder = holder as ItemViewHolder
            itemHolder.rating.visibility = View.GONE

            when (notif.getWooType()) {
                NEW_ORDER -> {
                    itemHolder.icon.setImageResource(R.drawable.ic_cart)
                }
                PRODUCT_REVIEW -> {
                    itemHolder.icon.setImageResource(R.drawable.ic_comment)

                    notif.getRating()?.let {
                        itemHolder.rating.rating = it
                        itemHolder.rating.visibility = View.VISIBLE
                    }
                }
                UNKNOWN -> WooLog.e(
                        NOTIFICATIONS,
                        "Unsupported woo notification type: ${notif.type} | ${notif.subtype}")
            }

            itemHolder.title.text = notif.getTitleSnippet()
            itemHolder.desc.text = notif.getMessageSnippet()

            itemHolder.itemView.setOnClickListener {
                listListener?.onNotificationClicked(notif)
            }
        }

        override fun getHeaderViewHolder(view: View) = HeaderViewHolder(view)

        override fun onBindHeaderViewHolder(holder: ViewHolder?) {
            val headerViewHolder = holder as HeaderViewHolder

            when (TimeGroup.valueOf(title)) {
                TimeGroup.GROUP_OLDER_MONTH -> headerViewHolder.title.setText(R.string.date_timeframe_older_month)
                TimeGroup.GROUP_OLDER_WEEK -> headerViewHolder.title.setText(R.string.date_timeframe_older_week)
                TimeGroup.GROUP_OLDER_TWO_DAYS -> headerViewHolder.title.setText(R.string.date_timeframe_older_two_days)
                TimeGroup.GROUP_YESTERDAY -> headerViewHolder.title.setText(R.string.date_timeframe_yesterday)
                TimeGroup.GROUP_TODAY -> headerViewHolder.title.setText(R.string.date_timeframe_today)
            }
        }
    }

    private class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var icon: ImageView = view.notif_icon
        var title: TextView = view.notif_title
        var desc: TextView = view.notif_desc
        var rating: RatingBar = view.notif_rating
    }

    private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.orderListHeader
    }

    class NotifsListItemDecoration(context: Context) : DividerItemDecoration(
            context,
            DividerItemDecoration.HORIZONTAL
    ) {
        private val dividerWidth = DisplayUtils.dpToPx(
                context,
                context.resources.getDimensionPixelSize(R.dimen.margin_small)
        ).toFloat()

        private var decorListener: ItemDecorationListener? = null
        private val bounds = Rect()

        fun setListener(listener: ItemDecorationListener?) {
            decorListener = listener
        }

        override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(child)
                val itemType = decorListener?.getItemTypeAtPosition(position) ?: ItemType.READ_NOTIF

                /*
                 * note that we have to draw the indicator for *all* items rather than just unread notifs
                 * in order to paint over recycled cells that have a previously-drawn indicator
                 */
                val colorId = when (itemType) {
                    ItemType.HEADER -> R.color.list_header_bg
                    ItemType.UNREAD_NOTIF -> R.color.wc_green
                    else -> R.color.list_item_bg
                }

                val paint = Paint()
                paint.color = ContextCompat.getColor(parent.context, colorId)

                parent.getDecoratedBoundsWithMargins(child, bounds)
                val top = bounds.top
                val bottom = bounds.bottom + Math.round(child.translationY)
                val left = bounds.left
                val right = left + dividerWidth

                canvas.drawRect(left.toFloat(), top.toFloat(), right, bottom.toFloat(), paint)
            }
        }
    }
}
