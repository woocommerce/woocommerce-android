package com.woocommerce.android.ui.base

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.ui.orders.OrderDetailFragment
import com.woocommerce.android.ui.orders.OrderFulfillmentFragment
import com.woocommerce.android.ui.orders.OrderProductListFragment
import com.woocommerce.android.ui.orders.OrdersViewRouter
import org.wordpress.android.fluxc.model.WCOrderModel

/**
 * Special interface for top-level fragments like those hosted by the bottom bar.
 * Adds an extra layer of management to ensure proper routing and handling of child
 * fragments and their associated back stack.
 */
interface TopLevelFragmentView : FragmentManager.OnBackStackChangedListener, OrdersViewRouter {
    var isActive: Boolean

    /**
     * Load the provided fragment into the current view and disable the main
     * underlying fragment view.
     *
     * @param fragment The child fragment to load
     * @param tag The fragment tag for recovering fragment from back stack
     */
    fun loadChildFragment(fragment: Fragment, tag: String)

    /**
     * Inflate the fragment view and return to be added to the parent
     * container.
     */
    fun onCreateFragmentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?

    /**
     * Return the title that should appear in the action bar while this fragment is
     * visible.
     */
    fun getFragmentTitle(): String

    /**
     * Refresh this top-level fragment data and reset its state.
     */
    fun refreshFragmentState()

    /**
     * Scroll to the top of this view
     */
    fun scrollToTop()

    /**
     * Pop fragments on back stack until the one labeled with this state tag
     * is reached.
     *
     * @return True if the state was found and all fragments above that state
     * in the back stack were popped. False if state not found - no action taken.
     */
    fun popToState(tag: String): Boolean

    /**
     * Closes the current child fragment by popping it from the backstack
     */
    fun closeCurrentChildFragment()

    /**
     * Locate a fragment on the back stack using the back stack tag provided.
     *
     * @return The fragment matching the provided tag, or null if not found.
     */
    fun getFragmentFromBackStack(tag: String): Fragment?

    /**
     * Only open the order detail if the list is not actively being refreshed.
     */
    override fun openOrderDetail(order: WCOrderModel, markOrderComplete: Boolean) {
        val tag = OrderDetailFragment.TAG
        getFragmentFromBackStack(tag)?.let {
            val args = it.arguments ?: Bundle()
            args.putString(OrderDetailFragment.FIELD_ORDER_IDENTIFIER, order.getIdentifier())
            args.putBoolean(OrderDetailFragment.FIELD_MARK_COMPLETE, markOrderComplete)
            it.arguments = args
            popToState(tag)
        } ?: loadChildFragment(
                OrderDetailFragment.newInstance(
                        orderId = order.getIdentifier(),
                        markComplete = markOrderComplete
                ), tag
        )
    }

    override fun openOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNotificationId: Long?) {
        val tag = OrderDetailFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(OrderDetailFragment.newInstance(localSiteId, remoteOrderId, remoteNotificationId), tag)
        }
    }

    override fun openOrderFulfillment(order: WCOrderModel) {
        val tag = OrderFulfillmentFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(OrderFulfillmentFragment.newInstance(order), tag)
        }
    }

    override fun openOrderProductList(order: WCOrderModel) {
        val tag = OrderProductListFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(OrderProductListFragment.newInstance(order), tag)
        }
    }
}
