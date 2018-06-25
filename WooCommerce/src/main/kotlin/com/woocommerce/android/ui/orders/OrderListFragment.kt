package com.woocommerce.android.ui.orders

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
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
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_order_list.*
import kotlinx.android.synthetic.main.fragment_order_list.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class OrderListFragment : TopLevelFragment(), OrderListContract.View {
    companion object {
        val TAG: String = OrderListFragment::class.java.simpleName
        const val STATE_KEY_LIST = "list-state"
        const val STATE_KEY_LOAD_PENDING = "is-load-pending"
        fun newInstance() = OrderListFragment()
    }

    @Inject lateinit var presenter: OrderListContract.Presenter
    @Inject lateinit var ordersAdapter: OrderListAdapter

    private lateinit var ordersDividerDecoration: DividerItemDecoration
    private lateinit var listLayoutAnimation: LayoutAnimationController

    private var loadOrdersPending = true // If true, the fragment will refresh its orders when its visible
    private var listState: Parcelable? = null // Save the state of the recycler view

    override var isActive: Boolean = false
        get() = childFragmentManager.backStackEntryCount == 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(STATE_KEY_LIST)
            loadOrdersPending = bundle.getBoolean(STATE_KEY_LOAD_PENDING, false)
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
                    loadOrdersPending = true
                    presenter.loadOrders(forceRefresh = true)
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
            presenter.loadOrders(forceRefresh = loadOrdersPending)
        }

        listState?.let {
            ordersList.layoutManager.onRestoreInstanceState(listState)
            listState = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val listState = ordersList.layoutManager.onSaveInstanceState()

        outState.putParcelable(STATE_KEY_LIST, listState)
        outState.putBoolean(STATE_KEY_LOAD_PENDING, loadOrdersPending)
        super.onSaveInstanceState(outState)
    }

    override fun onBackStackChanged() {
        super.onBackStackChanged()

        // If this fragment is now visible and we've deferred loading orders due to it not
        // being visible - go ahead and load the orders.
        if (isActive) {
            presenter.loadOrders(loadOrdersPending)
        }
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun setLoadingIndicator(active: Boolean) {
        with(orderRefreshLayout) {
            // Make sure this is called after the layout is done with everything else.
            post { isRefreshing = active }
        }
    }

    override fun setLoadingMoreIndicator(active: Boolean) {
        if (isAdded) {
            load_more_progressbar.visibility = if (active) View.VISIBLE else View.GONE
        }
    }

    override fun showOrders(orders: List<WCOrderModel>, clearExisting: Boolean) {
        ordersView.visibility = View.VISIBLE
        noOrdersView.visibility = View.GONE

        ordersList?.let { listView ->
            if (clearExisting) {
                ordersList.scrollToPosition(0)
                listView.layoutAnimation = listLayoutAnimation
            }
            ordersAdapter.setOrders(orders, clearExisting)
        }
        loadOrdersPending = false
    }

    override fun showNoOrders() {
        ordersView.visibility = View.GONE
        noOrdersView.visibility = View.VISIBLE
    }

    /**
     * Only open the order detail if the list is not actively being refreshed.
     */
    override fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean) {
        if (!orderRefreshLayout.isRefreshing) {
            val tag = OrderDetailFragment.TAG
            getFragmentFromBackStack(tag)?.let {
                val args = it.arguments ?: Bundle()
                args.putString(OrderDetailFragment.FIELD_ORDER_IDENTIFIER, order.getIdentifier())
                args.putString(OrderDetailFragment.FIELD_ORDER_NUMBER, order.number)
                args.putBoolean(OrderDetailFragment.FIELD_MARK_COMPLETE, markOrderComplete)
                it.arguments = args
                popToState(tag)
            } ?: loadChildFragment(OrderDetailFragment.newInstance(order, markOrderComplete), tag)
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
        loadOrdersPending = true
        if (isActive) {
            presenter.loadOrders(forceRefresh = true)
        }
    }

    // region OrderCustomerActionListener
    override fun dialPhone(phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_phone_app)
        }
    }

    override fun createEmail(emailAddr: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$emailAddr") // only email apps should handle this
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_email_app)
        }
    }

    override fun sendSms(phone: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:$phone")
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_sms_app)
        }
    }
    // endregion
}
