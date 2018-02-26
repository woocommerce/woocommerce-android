package com.woocommerce.android.ui.orders

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
        const val FIELD_LOCAL_ORDER_ID = "local-order-id"
        const val FIELD_REMOTE_ORDER_ID = "remote-order-id"
        val TAG: String = OrderDetailFragment::class.java.simpleName

        fun newInstance(localOrderId: Int, remoteOrderId: Long): Fragment {
            val args = Bundle()
            args.putInt(FIELD_LOCAL_ORDER_ID, localOrderId)
            args.putLong(FIELD_REMOTE_ORDER_ID, remoteOrderId)
            val fragment = OrderDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: OrderDetailContract.Presenter

    private var remoteOrderId: Long = 0L
    private var localOrderId: Int = 0

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (savedInstanceState == null) {
            localOrderId = arguments.getInt(FIELD_LOCAL_ORDER_ID, 0)
            remoteOrderId = arguments.getLong(FIELD_REMOTE_ORDER_ID, 0L)
        } else {
            localOrderId = savedInstanceState.getInt(FIELD_LOCAL_ORDER_ID)
            remoteOrderId = savedInstanceState.getLong(FIELD_REMOTE_ORDER_ID)
        }

        // Set the activity title
        activity.title = getString(R.string.order_orderstatus_ordernum, remoteOrderId)

        val view = inflater?.inflate(R.layout.fragment_order_detail, container, false)
        view?.let {
            with(view) {
                orderRefreshLayout.apply {
                    setColorSchemeColors(
                            ContextCompat.getColor(activity, R.color.colorPrimary),
                            ContextCompat.getColor(activity, R.color.colorAccent),
                            ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                    )

                    setOnRefreshListener {
                        presenter.loadOrderDetail(localOrderId)
                    }
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        presenter.loadOrderDetail(localOrderId)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putInt(FIELD_LOCAL_ORDER_ID, localOrderId)
        outState?.putLong(FIELD_REMOTE_ORDER_ID, remoteOrderId)
        super.onSaveInstanceState(outState)
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

    override fun showOrderDetail(order: WCOrderModel?) {
        order?.let {
            // Populate the Order Status Card
            orderDetail_orderStatus.initView(order)

            // Populate the Customer Information Card
            orderDetail_customerInfo.initView(order)
        }
    }
}
