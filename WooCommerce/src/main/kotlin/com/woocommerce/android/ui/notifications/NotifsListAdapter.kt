package com.woocommerce.android.ui.notifications

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.extensions.WooNotificationType.NEW_ORDER
import com.woocommerce.android.extensions.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.extensions.WooNotificationType.UNKNOWN
import com.woocommerce.android.extensions.getMessageSnippet
import com.woocommerce.android.extensions.getRating
import com.woocommerce.android.extensions.getTitleSnippet
import com.woocommerce.android.extensions.getWooType
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.widgets.SectionParameters
import com.woocommerce.android.widgets.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.StatelessSection
import kotlinx.android.synthetic.main.notifs_list_item.view.*
import kotlinx.android.synthetic.main.order_list_header.view.*
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

class NotifsListAdapter @Inject constructor(val presenter: NotifsListPresenter) : SectionedRecyclerViewAdapter() {
    interface ReviewListListener {
        fun onNotificationClicked(notification: NotificationModel)
    }

    private val notifsList: ArrayList<NotificationModel> = ArrayList()
    private var listener: ReviewListListener? = null

    fun setListener(listener: ReviewListListener) {
        this.listener = listener
    }

    fun setNotifications(notifs: List<NotificationModel>) {
        // clear all the current data from the adapter
        removeAllSections()

        // Build a notifs for each [TimeGroup] section
        val listToday = ArrayList<NotificationModel>()
        val listYesterday = ArrayList<NotificationModel>()
        val listTwoDays = ArrayList<NotificationModel>()
        val listWeek = ArrayList<NotificationModel>()
        val listMonth = ArrayList<NotificationModel>()

        notifs.forEach {
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
        notifsList.addAll(notifs)
    }

    fun isSameList(notifs: List<NotificationModel>): Boolean {
        if (notifs.size != notifsList.size) {
            return false
        }

        val didMatch = fun(notification: NotificationModel): Boolean {
            notifsList.forEach {
                if (it.noteId == notification.noteId &&
                        it.title == notification.title &&
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

    fun clearAdapterData() {
        removeAllSections()
        notifsList.clear()
        notifyDataSetChanged()
    }

    private inner class NotifsListSection(val title: String, val list: List<NotificationModel>) : StatelessSection(
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
                listener?.onNotificationClicked(notif)
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
}
