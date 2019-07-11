package com.woocommerce.android.ui.orders

import android.content.Context
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.widgets.BadgedItemDecoration.ItemType
import com.woocommerce.android.widgets.FlowLayout
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.sectionedrecyclerview.StatelessSection
import com.woocommerce.android.widgets.tags.TagView
import kotlinx.android.synthetic.main.order_list_header.view.*
import kotlinx.android.synthetic.main.order_list_item.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

/**
 * Adapter serves up list of [WCOrderModel] items grouped by the appropriate [TimeGroup].
 */
class OrderListAdapter @Inject constructor(
    val presenter: OrderListContract.Presenter,
    val currencyFormatter: CurrencyFormatter
)
    : SectionedRecyclerViewAdapter() {
    interface OnLoadMoreListener {
        fun onRequestLoadMore()
    }

    private var loadMoreListener: OnLoadMoreListener? = null
    private val orderList: ArrayList<WCOrderModel> = ArrayList()
    var orderStatusFilter: String? = null
    private var orderStatusOptionsMap: Map<String, WCOrderStatusModel> = emptyMap()

    fun setOnLoadMoreListener(listener: OnLoadMoreListener?) {
        loadMoreListener = listener
    }

    fun setOrders(orders: List<WCOrderModel>, filterByStatus: String? = null) {
        orderStatusFilter = filterByStatus
        orderStatusOptionsMap = presenter.getOrderStatusOptions()

        // clear all the current data from the adapter
        removeAllSections()

        // Build a list for each [TimeGroup] section
        val listToday = ArrayList<WCOrderModel>()
        val listYesterday = ArrayList<WCOrderModel>()
        val listTwoDays = ArrayList<WCOrderModel>()
        val listWeek = ArrayList<WCOrderModel>()
        val listMonth = ArrayList<WCOrderModel>()

        orders.forEach {
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

        // we want to retain the sorted list of orders so we can correctly match them by position
        // in getItemTypeAtRecyclerPosition
        with (orderList) {
            clear()
            addAll(listToday)
            addAll(listYesterday)
            addAll(listTwoDays)
            addAll(listWeek)
            addAll(listMonth)
        }

        if (listToday.size > 0) {
            addSection(OrderListSection(TimeGroup.GROUP_TODAY.name, listToday))
        }

        if (listYesterday.size > 0) {
            addSection(OrderListSection(TimeGroup.GROUP_YESTERDAY.name, listYesterday))
        }

        if (listTwoDays.size > 0) {
            addSection(OrderListSection(TimeGroup.GROUP_OLDER_TWO_DAYS.name, listTwoDays))
        }

        if (listWeek.size > 0) {
            addSection(OrderListSection(TimeGroup.GROUP_OLDER_WEEK.name, listWeek))
        }

        if (listMonth.size > 0) {
            addSection(OrderListSection(TimeGroup.GROUP_OLDER_MONTH.name, listMonth))
        }

        notifyDataSetChanged()
    }

    fun setOrderStatusOptions(orderStatusOptions: Map<String, WCOrderStatusModel>) {
        this.orderStatusOptionsMap = orderStatusOptions
        notifyDataSetChanged()
    }

    /**
     * Adds the passed list of orders to the current list, making sure not to add orders that
     * are already in the current list
     */
    fun addOrders(orders: List<WCOrderModel>) {
        if (orders.isEmpty()) return

        val allOrders = ArrayList<WCOrderModel>()
        allOrders.addAll(orderList)

        orders.forEach {
            if (!containsOrder(it)) {
                allOrders.add(it)
            }
        }

        if (allOrders.size > orderList.size) {
            setOrders(allOrders)
        }
    }

    /**
     * Returns true if the passed order is in the current list of orders
     */
    private fun containsOrder(order: WCOrderModel): Boolean {
        orderList.forEach {
            if (it.remoteOrderId == order.remoteOrderId && it.status == order.status) {
                return true
            }
        }
        return false
    }

    /**
     * returns true if the passed list of orders is the same as the current list
     */
    fun isSameOrderList(orders: List<WCOrderModel>): Boolean {
        if (orders.size != orderList.size) {
            return false
        }

        orders.forEach {
            if (!containsOrder(it)) {
                return false
            }
        }

        return true
    }

    fun clearAdapterData() {
        if (orderList.isNotEmpty()) {
            removeAllSections()
            orderList.clear()
            notifyDataSetChanged()
        }
    }

    /**
     * returns true if the
     * @param title matches the title of one of the sections in the list
     */
    fun isSectionAvailable(title: String): Boolean {
        for (entry in sectionsMap) {
            val section = entry.value as? OrderListSection
            if (title == section?.title) {
                return true
            }
        }
        return false
    }

    /**
     * returns the total section item count give the
     * @param title
     */
    fun getSectionItemsTotal(title: String): Int {
        for (entry in sectionsMap) {
            val section = entry.value as? OrderListSection
            if (title == section?.title) {
                return section.list.size
            }
        }
        return 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        if (position == itemCount - 1) {
            loadMoreListener?.onRequestLoadMore()
        }
    }

    /**
     * Returns the order date formatted as a date string, or null if the date is missing or invalid.
     * Note that the year is not shown when it's the same as the current one
     */
    private fun getFormattedOrderDate(context: Context, order: WCOrderModel): String? {
        DateTimeUtils.dateUTCFromIso8601(order.dateCreated)?.let { date ->
            val flags = if (DateTimeUtils.isSameYear(date, DateTimeUtils.nowUTC())) {
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR
            } else {
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
            }
            return DateUtils.formatDateTime(context, date.time, flags)
        } ?: return null
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
     * Returns the type of item at the passed position so the badged item decoration
     * can badge orders that haven't been fulfilled
     *
     * @param position position of the item in the recycler
     */
    fun getItemTypeAtRecyclerPosition(position: Int): ItemType {
        if (isHeaderAtRecyclerPosition(position)) {
            return ItemType.HEADER
        }

        var currentPos = 0
        for (order in orderList) {
            if (isHeaderAtRecyclerPosition(currentPos)) {
                currentPos++
            }
            if (currentPos == position) {
                return if (order.status.equals(CoreOrderStatus.PROCESSING.value, true)) {
                    ItemType.BADGED
                } else {
                    ItemType.UNBADGED
                }
            }
            currentPos++
        }

        WooLog.w(WooLog.T.NOTIFICATIONS, "Failed to get item type at notifs recycler position $position")
        return ItemType.UNBADGED
    }

    /**
     * Custom class represents a single [TimeGroup] and it's assigned list of [WCOrderModel]. Responsible
     * for providing and populating the header and item view holders.
     */
    private inner class OrderListSection(val title: String, val list: List<WCOrderModel>) : StatelessSection(
            SectionParameters.Builder(R.layout.order_list_item).headerResourceId(R.layout.order_list_header).build()
    ) {
        override fun getContentItemsTotal() = list.size

        override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
            return ItemViewHolder(view)
        }

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val order = list[position]
            val itemHolder = holder as ItemViewHolder
            val resources = itemHolder.rootView.context.applicationContext.resources
            val ctx = itemHolder.rootView.context

            val dateStr = getFormattedOrderDate(ctx, order)
            if (dateStr != null) {
                itemHolder.orderDate.text = dateStr
                itemHolder.orderDate.visibility = View.VISIBLE
            } else {
                itemHolder.orderDate.visibility = View.GONE
            }

            itemHolder.orderNum.text = resources.getString(R.string.orderlist_item_order_num, order.number)
            itemHolder.orderName.text = resources.getString(
                    R.string.orderlist_item_order_name, order.billingFirstName, order.billingLastName)
            itemHolder.orderTotal.text = currencyFormatter.formatCurrency(order.total, order.currency)
            itemHolder.rootView.tag = order
            itemHolder.rootView.setOnClickListener {
                val orderItem = it.tag as WCOrderModel
                presenter.openOrderDetail(orderItem)
            }
            // clear existing tags and add new ones
            itemHolder.orderTagList.removeAllViews()
            val orderStatus = orderStatusOptionsMap[order.status]
                    ?: refreshOrderStatusOptionsAndCreateTemp(order.status)
            processTagView(ctx, orderStatus, itemHolder)
        }

        private fun refreshOrderStatusOptionsAndCreateTemp(status: String): WCOrderStatusModel {
            presenter.refreshOrderStatusOptions()
            return WCOrderStatusModel().apply {
                statusKey = status
                label = status
            }
        }

        override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
            return HeaderViewHolder(view)
        }

        override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
            val headerViewHolder = holder as HeaderViewHolder

            when (TimeGroup.valueOf(title)) {
                TimeGroup.GROUP_OLDER_MONTH -> headerViewHolder.title.setText(R.string.date_timeframe_older_month)
                TimeGroup.GROUP_OLDER_WEEK -> headerViewHolder.title.setText(R.string.date_timeframe_older_week)
                TimeGroup.GROUP_OLDER_TWO_DAYS -> headerViewHolder.title.setText(R.string.date_timeframe_older_two_days)
                TimeGroup.GROUP_YESTERDAY -> headerViewHolder.title.setText(R.string.date_timeframe_yesterday)
                TimeGroup.GROUP_TODAY -> headerViewHolder.title.setText(R.string.date_timeframe_today)
            }
        }

        /**
         * Converts the order status label into an [OrderStatusTag], creates the associated [TagView],
         * and add it to the holder. No need to trim the label text since this is done in [OrderStatusTag]
         */
        private fun processTagView(ctx: Context, orderStatus: WCOrderStatusModel, holder: ItemViewHolder) {
            val orderTag = OrderStatusTag(orderStatus)
            val tagView = TagView(ctx)
            tagView.tag = orderTag
            holder.orderTagList.addView(tagView)
        }
    }

    private class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var orderDate: TextView = view.orderDate
        var orderNum: TextView = view.orderNum
        var orderName: TextView = view.orderName
        var orderTotal: TextView = view.orderTotal
        var orderTagList: FlowLayout = view.orderTags
        var rootView = view
    }

    private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.orderListHeader
    }
}
