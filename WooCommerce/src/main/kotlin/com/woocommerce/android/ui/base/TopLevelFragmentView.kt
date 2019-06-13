package com.woocommerce.android.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.ui.orders.AddOrderNoteFragment
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingFragment
import com.woocommerce.android.ui.orders.OrderFulfillmentFragment
import com.woocommerce.android.ui.orders.OrderProductListFragment
import com.woocommerce.android.ui.orders.OrdersViewRouter
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier

/**
 * Special interface for top-level fragments like those hosted by the bottom bar.
 * Adds an extra layer of management to ensure proper routing and handling of child
 * fragments and their associated back stack.
 */
interface TopLevelFragmentView : androidx.fragment.app.FragmentManager.OnBackStackChangedListener, OrdersViewRouter {
    var isActive: Boolean

    /**
     * Load the provided fragment into the current view and disable the main
     * underlying fragment view.
     *
     * @param fragment The child fragment to load
     * @param tag The fragment tag for recovering fragment from back stack
     */
    fun loadChildFragment(fragment: androidx.fragment.app.Fragment, tag: String)

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
    fun getFragmentFromBackStack(tag: String): androidx.fragment.app.Fragment?

    override fun openOrderFulfillment(order: WCOrderModel, isUsingCachedShipmentTrackings: Boolean) {
        val tag = OrderFulfillmentFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(OrderFulfillmentFragment.newInstance(order, isUsingCachedShipmentTrackings), tag)
        }
    }

    override fun openOrderProductList(order: WCOrderModel) {
        val tag = OrderProductListFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(OrderProductListFragment.newInstance(order), tag)
        }
    }

    override fun openAddOrderNote(order: WCOrderModel) {
        val tag = AddOrderNoteFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(AddOrderNoteFragment.newInstance(order), tag)
        }
    }

    override fun openAddOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        orderTrackingProvider: String,
        isCustomProvider: Boolean
    ) {
        val tag = AddOrderShipmentTrackingFragment.TAG
        if (!popToState(tag)) {
            loadChildFragment(
                    AddOrderShipmentTrackingFragment.newInstance(
                            orderIdentifier,
                            orderTrackingProvider,
                            isCustomProvider
                    ), tag
            )
        }
    }
}
