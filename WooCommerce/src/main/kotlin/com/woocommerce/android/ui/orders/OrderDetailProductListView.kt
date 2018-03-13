package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_product_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import java.util.Currency

class OrderDetailProductListView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_product_list, this)
    }

    fun initView(order: WCOrderModel) {
        // get the proper currency symbol for the current order
        var currencySymbol = ""
        try {
            currencySymbol = Currency.getInstance(order.currency).symbol
        } catch (e: IllegalArgumentException) {
            Log.e(OrderListAdapter.TAG, "Error finding valid currency symbol for currency code [${order.currency}]", e)
        }

        val viewManager = LinearLayoutManager(context)
        val viewAdapter = ProductListAdapter(order.getLineItemList(), currencySymbol)

        productList_products.apply {
            setHasFixedSize(false)
            layoutManager = viewManager
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = viewAdapter
        }
    }

    class ProductListAdapter(private val orderItems: List<WCOrderModel.LineItem>, private val currencySymbol: String) :
            RecyclerView.Adapter<ProductListAdapter.ViewHolder>() {
        class ViewHolder(val view: OrderDetailProductItemView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: OrderDetailProductItemView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_detail_product_list_item, parent, false)
                    as OrderDetailProductItemView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.view.initView(orderItems[position], currencySymbol)
        }

        override fun getItemCount(): Int {
            return orderItems.size
        }
    }
}
