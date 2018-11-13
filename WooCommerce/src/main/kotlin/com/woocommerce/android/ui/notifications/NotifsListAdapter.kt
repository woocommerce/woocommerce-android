package com.woocommerce.android.ui.notifications

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.ui.notifications.WCNotificationModel.Order
import com.woocommerce.android.ui.notifications.WCNotificationModel.Review
import com.woocommerce.android.widgets.SectionParameters
import com.woocommerce.android.widgets.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.StatelessSection
import kotlinx.android.synthetic.main.notifs_list_item.view.*
import kotlinx.android.synthetic.main.order_list_header.view.*
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

class NotifsListAdapter @Inject constructor(val presenter: NotifsListPresenter) : SectionedRecyclerViewAdapter() {
    interface ReviewListListener {
        fun onNotificationClicked(notification: WCNotificationModel)
    }

    private val notifsList: ArrayList<WCNotificationModel> = ArrayList()
    private var listener: ReviewListListener? = null

    fun setListener(listener: ReviewListListener) {
        this.listener = listener
    }

    fun setNotifications(notifs: List<WCNotificationModel>) {
        // clear all the current data from the adapter
        removeAllSections()

        // Build a notifs for each [TimeGroup] section
        val listToday = ArrayList<WCNotificationModel>()
        val listYesterday = ArrayList<WCNotificationModel>()
        val listTwoDays = ArrayList<WCNotificationModel>()
        val listWeek = ArrayList<WCNotificationModel>()
        val listMonth = ArrayList<WCNotificationModel>()

        notifs.forEach {
            // Default to today if the date cannot be parsed
            val date: Date = DateTimeUtils.dateUTCFromIso8601(it.dateCreated) ?: Date()
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

    fun isSameList(notifs: List<WCNotificationModel>): Boolean {
        if (notifs.size != notifsList.size) {
            return false
        }

        val didMatch = fun(notification: WCNotificationModel): Boolean {
            notifsList.forEach {
                if (it.id == notification.id && it.title == notification.title && it.desc == notification.desc) {
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (presenter.canLoadMore() && !presenter.isLoading() && position == itemCount - 1) {
            presenter.loadMoreNotifs()
        }
    }

    private inner class NotifsListSection(val title: String, val list: List<WCNotificationModel>) : StatelessSection(
            SectionParameters.Builder(R.layout.notifs_list_item).headerResourceId(R.layout.order_list_header).build()
    ) {
        override fun getContentItemsTotal() = list.size

        override fun getItemViewHolder(view: View) = ItemViewHolder(view)

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val notif = list[position]
            val itemHolder = holder as ItemViewHolder

            when (notif) {
                is Order -> {
                    itemHolder.rating.visibility = View.GONE
                    itemHolder.icon.setImageResource(R.drawable.ic_cart)
                }
                is Review -> {
                    itemHolder.icon.setImageResource(R.drawable.ic_comment)
                    itemHolder.rating.visibility = View.VISIBLE
                    itemHolder.rating.rating = notif.rating
                }
            }

            itemHolder.title.text = notif.title
            itemHolder.desc.text = notif.desc
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
