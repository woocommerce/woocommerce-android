package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

abstract class BaseOrderEditingFragment : BaseFragment, BackPressListener {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    protected val sharedViewModel by hiltNavGraphViewModels<OrderEditingViewModel>(R.id.nav_graph_orders)
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    protected var doneMenuItem: MenuItem? = null

    /**
     * The value to pass to analytics for the specific screen, used to record when the user enters or
     * exits the screen. Should be one of:
     *      AnalyticsTracker.ORDER_EDIT_CUSTOMER_NOTE
     *      AnalyticsTracker.ORDER_EDIT_SHIPPING_ADDRESS
     *      AnalyticsTracker.ORDER_EDIT_BILLING_ADDRESS
     */
    abstract val analyticsValue: String

    /**
     * This TextWatcher can be used to detect EditText changes in any order editing fragment
     */
    protected val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            // noop
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // noop
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateDoneMenuItem()
        }
    }

    /**
     * Descendants should return true if the user made any changes
     */
    abstract fun hasChanges(): Boolean

    /**
     * Descendants should override this to tell the shared view model to save specific changes. Note that
     * since we're using optimistic updating, a True result doesn't necessarily mean the update succeeded,
     * just that it was sent. A False result means the request couldn't be sent, either due to connection
     * problems or validation issues.
     */
    abstract fun saveChanges(): Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (savedInstanceState == null) {
            trackEventStarted()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateDoneMenuItem()
    }

    private fun setupObservers() {
        sharedViewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is MultiLiveEvent.Event.ShowSnackbar -> {
                        uiMessageResolver.showSnack(event.message)
                    }
                }
            }
        )

        sharedViewModel.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                if (saveChanges()) {
                    navigateUp()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    protected fun updateDoneMenuItem() {
        doneMenuItem?.isVisible = hasChanges()
    }

    override fun onRequestAllowBackPress(): Boolean {
        return if (hasChanges()) {
            confirmDiscard()
            false
        } else {
            trackEventCanceled()
            true
        }
    }

    private fun confirmDiscard() {
        MultiLiveEvent.Event.ShowDialog.buildDiscardDialogEvent(
            positiveBtnAction = { _, _ ->
                navigateUp()
            }
        ).showDialog()
    }

    protected fun navigateUp() {
        trackEventCanceled()
        ActivityUtils.hideKeyboard(activity)
        findNavController().navigateUp()
    }

    private fun trackEventStarted() {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.ORDER_DETAIL_EDIT_FLOW_STARTED,
            mapOf(
                AnalyticsTracker.KEY_SUBJECT to analyticsValue
            )
        )
    }

    private fun trackEventCanceled() {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.ORDER_DETAIL_EDIT_FLOW_CANCELED,
            mapOf(
                AnalyticsTracker.KEY_SUBJECT to analyticsValue
            )
        )
    }
}
