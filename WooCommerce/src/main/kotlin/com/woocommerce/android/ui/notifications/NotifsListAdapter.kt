package com.woocommerce.android.ui.notifications

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
import com.woocommerce.android.widgets.BadgedItemDecoration.ItemType
import com.woocommerce.android.widgets.sectionedrecyclerview.Section
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.sectionedrecyclerview.StatelessSection
import kotlinx.android.synthetic.main.notifs_list_item.view.*
import kotlinx.android.synthetic.main.order_list_header.view.*
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import java.util.HashSet
import javax.inject.Inject

class NotifsListAdapter @Inject constructor(context: Context) : SectionedRecyclerViewAdapter() {
    private var starTintColor: Int = 0
    init {
        starTintColor = ContextCompat.getColor(context, R.color.grey_darken_30)
    }

    interface ReviewListListener {
        fun onNotificationClicked(notification: NotificationModel)
    }

    private val notifsList = mutableListOf<NotificationModel>()
    private var listListener: ReviewListListener? = null

    // Copy of current notification manually removed from the list so the action may be undone.
    private var pendingRemovalNotification: Triple<NotificationModel, NotifsListSection, Int>? = null

    // List of all remote note IDs the user has removed this session
    private val removedRemoteIds = HashSet<Long>()

    // region Public methods
    fun setListListener(listener: ReviewListListener) {
        listListener = listener
    }

    fun setNotifications(notifs: List<NotificationModel>) {
        // make sure to exclude any notifs that we know have been removed
        val newList = notifs.filter { !removedRemoteIds.contains(it.remoteNoteId) }

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
        val posInList = notifsList.indexOfFirst { it.remoteNoteId == remoteNoteId }
        if (posInList == -1) {
            WooLog.w(T.NOTIFICATIONS, "Unable to hide notification, position is -1")
            pendingRemovalNotification = null
            removedRemoteIds.remove(remoteNoteId)
            return
        }

        getSectionForListItemPosition(posInList)?.let {
            val section = it as NotifsListSection
            val posInSection = getPositionInSectionByListPos(posInList)
            pendingRemovalNotification = Triple(notifsList[posInList], section, posInSection)
            removedRemoteIds.add(remoteNoteId)

            // remove from the section list
            section.list.removeAt(posInSection)
            notifyItemRemovedFromSection(section, posInSection)

            if (section.list.size == 0) {
                val sectionPos = getSectionPosition(section)
                section.isVisible = false
                if (sectionPos != SectionedRecyclerViewAdapter.INVALID_POSITION) {
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
     * Returns the type of item at the passed position
     *
     * @param position position of the item in the recycler
     */
    fun getItemTypeAtRecyclerPosition(position: Int): ItemType {
        if (isHeaderAtRecyclerPosition(position)) {
            return ItemType.HEADER
        }

        var currentPos = 0
        for (notif in notifsList) {
            if (isHeaderAtRecyclerPosition(currentPos)) {
                currentPos++
            }
            if (currentPos == position) {
                return if (notif.read) ItemType.UNBADGED else ItemType.BADGED
            }
            currentPos++
        }

        WooLog.w(T.NOTIFICATIONS, "Failed to get item type at recycler position $position")
        return ItemType.UNBADGED
    }

    /**
     * Inserts the previously removed notification and notifies the recycler view.
     * @return The position in the adapter the item was added to
     */
    fun revertHiddenNotificationAndReturnPos(): Int {
        return pendingRemovalNotification?.let { (notif, section, pos) ->
            if (!section.isVisible) {
                section.isVisible = true
                notifySectionChangedToVisible(section)
            }

            with(section.list) {
                if (pos < size) {
                    add(pos, notif)
                } else {
                    add(notif)
                }
            }

            removedRemoteIds.remove(notif.remoteNoteId)
            pendingRemovalNotification = null

            notifyItemInsertedInSection(section, pos)
            getPositionInAdapter(section, pos)
        } ?: INVALID_POSITION
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

    fun isEmpty() = notifsList.isEmpty()
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
        WooLog.w(T.NOTIFS, "Unable to find matching section for position $position")
        return null
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

        override fun onBindItemViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
            val notif = list[position]
            val itemHolder = holder as ItemViewHolder
            itemHolder.rating.visibility = View.GONE

            when (notif.getWooType()) {
                NEW_ORDER -> {
                    itemHolder.icon.setImageResource(R.drawable.ic_cart)
                    itemHolder.desc.maxLines = Int.MAX_VALUE
                }
                PRODUCT_REVIEW -> {
                    itemHolder.icon.setImageResource(R.drawable.ic_comment)
                    itemHolder.desc.maxLines = 2

                    notif.getRating()?.let {
                        itemHolder.rating.rating = it
                        itemHolder.rating.visibility = View.VISIBLE
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            val stars = itemHolder.rating.progressDrawable as? LayerDrawable
                            stars?.getDrawable(2)?.setColorFilter(starTintColor, PorterDuff.Mode.SRC_ATOP)
                        }
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

        override fun onBindHeaderViewHolder(holder: ViewHolder) {
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
}
