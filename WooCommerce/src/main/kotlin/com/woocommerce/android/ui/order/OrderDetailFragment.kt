package com.woocommerce.android.ui.order

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_detail.*
import kotlinx.android.synthetic.main.fragment_order_detail.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View {
    companion object {
        const val FIELD_ORDER_ID = "order-id"
        const val FIELD_ORDER_NUMBER = "order-number"
        fun newInstance(order: WCOrderModel): Fragment {
            val args = Bundle()
            args.putInt(FIELD_ORDER_ID, order.id)

            // Use for populating the title only, not for record retrieval
            args.putLong(FIELD_ORDER_NUMBER, order.remoteOrderId)
            val fragment = OrderDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderDetailContract.Presenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_order_detail, container, false)

        arguments?.let { arguments ->
            val orderId = arguments.getInt(FIELD_ORDER_ID, 0)
            val orderNumber = arguments.getLong(FIELD_ORDER_NUMBER, 0L)

            // Set activity title
            activity?.title = getString(R.string.wc_order_orderNum, orderNumber)

            with(view) {
                orderRefreshLayout?.apply {
                    activity?.let {activity ->
                        setColorSchemeColors(
                                ContextCompat.getColor(activity, R.color.colorPrimary),
                                ContextCompat.getColor(activity, R.color.colorAccent),
                                ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                        )
                    }

                    setOnRefreshListener {
                        presenter.loadOrderDetail(orderId)
                    }
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        val orderId = arguments?.getInt(FIELD_ORDER_ID, 0)
        orderId?.let {
            presenter.loadOrderDetail(it)
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun setLoadingIndicator(active: Boolean) {
        with(orderRefreshLayout) {
            post { isRefreshing = active }
        }
    }
}
