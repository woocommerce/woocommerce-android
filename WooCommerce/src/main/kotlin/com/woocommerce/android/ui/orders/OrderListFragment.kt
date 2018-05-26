package com.woocommerce.android.ui.orders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.main.MainUIMessageResolver
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import javax.inject.Inject

class OrderListFragment : TopLevelFragment(), OrderListContract.View, View.OnClickListener {
    companion object {
        val TAG: String = OrderListFragment::class.java.simpleName
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_ISINIT = "is-init"
        fun newInstance() = OrderListFragment()
    }

    @Inject lateinit var presenter: OrderListContract.Presenter
    @Inject lateinit var ordersAdapter: OrderListAdapter
    @Inject lateinit var uiResolver: MainUIMessageResolver

    private lateinit var ordersDividerDecoration: DividerItemDecoration
    private lateinit var listLayoutAnimation: LayoutAnimationController

    private var loadOrdersPending = false // If true, the fragment will refresh its orders when its visible
    private var listState: Parcelable? = null // Save the state of the recycler view
    private var isInit = false
    private var snackbar: Snackbar? = null // Displays connection errors

    override var isActive: Boolean = false
        get() = childFragmentManager.backStackEntryCount == 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(STATE_KEY_LIST)
            isInit = bundle.getBoolean(STATE_KEY_ISINIT, false)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateFragmentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_order_list, container, false)
        with(view) {
            orderRefreshLayout?.apply {
                activity?.let { activity ->
                    setColorSchemeColors(
                            ContextCompat.getColor(activity, R.color.colorPrimary),
                            ContextCompat.getColor(activity, R.color.colorAccent),
                            ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                    )
                }
                // Set the scrolling view in the custom SwipeRefreshLayout
                scrollUpChild = ordersList
                setOnRefreshListener {
                    presenter.loadOrders(context, forceRefresh = true)
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the divider decoration for the list
        ordersDividerDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)

        // Set the animation for this list. Gets disabled at various points so we use a variable for it.
        listLayoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fall_down)

        ordersList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            addItemDecoration(ordersDividerDecoration)
            adapter = ordersAdapter
            layoutAnimationListener = object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    // Remove the layout animation to prevent the animation from playing
                    // when just restoring the state of the recycler view.
                    layoutAnimation = null
                }

                override fun onAnimationStart(animation: Animation?) {}
            }
        }

        presenter.takeView(this)
        if (isActive) {
            presenter.loadOrders(context, forceRefresh = !isInit)
        } else {
            loadOrdersPending = true
        }
        listState?.let {
            ordersList.layoutManager.onRestoreInstanceState(listState)
            listState = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState = ordersList.layoutManager.onSaveInstanceState()

        snackbar?.let { isInit = false } // force an attempt to refresh view when back in foreground.
        outState.putParcelable(STATE_KEY_LIST, listState)
        outState.putBoolean(STATE_KEY_ISINIT, isInit)
        super.onSaveInstanceState(outState)
    }

    override fun onBackStackChanged() {
        super.onBackStackChanged()
        if (isActive) {
            // If this fragment is now visible and we've deferred loading orders due to it not
            // being visible or the connection error snackbar is not null (meaning we tried to fetch
            // orders before but were not successful) - go ahead and load the orders.
            if (loadOrdersPending || snackbar != null) {
                snackbar?.let { isInit = false } // Force a refresh if the connection error snackbar was present
                loadOrdersPending = false
                snackbar = null
                presenter.loadOrders(context, forceRefresh = !isInit)
            }
        } else {
            snackbar?.dismiss()
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        snackbar?.let {
            it.dismiss()
            snackbar = null
        }
        super.onDestroyView()
    }

    override fun setLoadingIndicator(active: Boolean) {
        with(orderRefreshLayout) {
            // Make sure this is called after the layout is done with everything else.
            post { isRefreshing = active }
        }
    }

    override fun showOrders(orders: List<WCOrderModel>, isForceRefresh: Boolean) {
        ordersView.visibility = View.VISIBLE
        noOrdersView.visibility = View.GONE

        ordersList?.let { listView ->
            if (isForceRefresh) {
                ordersList.scrollToPosition(0)
                listView.layoutAnimation = listLayoutAnimation
            }
            ordersAdapter.setOrders(orders)
        }
        isInit = true
    }

    override fun showNoOrders() {
        ordersView.visibility = View.GONE
        noOrdersView.visibility = View.VISIBLE
    }

    /**
     * Only open the order detail if the list is not actively being refreshed.
     */
    override fun openOrderDetail(order: WCOrderModel, originalOrderStatus: String?) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderDetailFragment.TAG
            getFragmentFromBackStack(tag)?.let {
                val args = it.arguments ?: Bundle()
                args.putString(OrderDetailFragment.FIELD_ORDER_IDENTIFIER, order.getIdentifier())
                args.putString(OrderDetailFragment.FIELD_ORDER_NUMBER, order.number)
                args.putString(OrderDetailFragment.FIELD_ORIGINAL_ORDER_STATUS, originalOrderStatus)
                it.arguments = args
                popToState(tag)
            } ?: loadChildFragment(OrderDetailFragment.newInstance(order, originalOrderStatus), tag)
        }
    }

    override fun openOrderFulfillment(order: WCOrderModel) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderFulfillmentFragment.TAG
            if (!popToState(tag)) {
                loadChildFragment(OrderFulfillmentFragment.newInstance(order), tag)
            }
        }
    }

    override fun openOrderProductList(order: WCOrderModel) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderProductListFragment.TAG
            if (!popToState(tag)) {
                loadChildFragment(OrderProductListFragment.newInstance(order), tag)
            }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.orders)
    }

    override fun refreshFragmentState() {
        isInit = false
        if (isActive) {
            presenter.loadOrders(context, forceRefresh = true)
        } else {
            loadOrdersPending = true
        }
    }

    override fun onClick(v: View?) {
        // Handler for the Connection Error RETRY button
        v?.let {
            snackbar?.dismiss()
            snackbar = null
            presenter.loadOrders(context, true)
        }
    }

    override fun showNetworkErrorFetchOrders() {
        ordersList?.let {
            snackbar = uiResolver.getRetrySnack(R.string.orderlist_error_network, null, this)
            snackbar?.show()
        }
    }

    // region OrderCustomerActionListener
    override fun dialPhone(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        startActivity(intent)
    }

    override fun createEmail(emailAddr: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$emailAddr") // only email apps should handle this
        context?.let { context ->
            if (intent.resolveActivity(context.packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    override fun sendSms(phone: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phone")
        context?.let { context ->
            if (intent.resolveActivity(context.packageManager) != null) {
                startActivity(intent)
            }
        }
    }
    // endregion
}
