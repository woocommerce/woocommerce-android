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
import javax.inject.Inject

class OrderDetailFragment : Fragment(), OrderDetailContract.View {
    companion object {
        const val FIELD_ORDER_ID = "order-id"
        val TAG: String = OrderDetailFragment::class.java.simpleName

        fun newInstance(orderId: Long): Fragment {
            val args = Bundle()
            args.putLong(FIELD_ORDER_ID, orderId)
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

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Set the title in the action bar
        val orderNumber = arguments.getLong(FIELD_ORDER_ID, 0L)
        activity.title = getString(R.string.wc_order_orderNum, orderNumber)

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
                        presenter.loadOrderDetail(orderNumber)
                    }
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)
        val orderNumber = arguments.getLong(FIELD_ORDER_ID, 0L)
        presenter.loadOrderDetail(orderNumber)
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
