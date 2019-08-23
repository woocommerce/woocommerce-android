package com.woocommerce.android.ui.orders

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_ADD_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.AnalyticsUtils
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_add_order_note.*
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class AddOrderNoteFragment : BaseFragment(), AddOrderNoteContract.View, BackPressListener {
    companion object {
        const val TAG = "AddOrderNoteFragment"
        private const val FIELD_NOTE_TEXT = "note_text"
        private const val FIELD_IS_CUSTOMER_NOTE = "is_customer_note"
        private const val FIELD_IS_CONFIRMING_DISCARD = "is_confirming_discard"
    }

    @Inject lateinit var presenter: AddOrderNoteContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var orderId: OrderIdentifier
    private lateinit var orderNumber: String

    private var isConfirmingDiscard = false
    private var shouldShowDiscardDialog = true

    private val navArgs: AddOrderNoteFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setRetainInstance(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_order_note, container, false)
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        orderId = navArgs.orderId
        orderNumber = navArgs.orderNumber

        savedInstanceState?.let { state ->
            addNote_switch.isChecked = state.getBoolean(FIELD_IS_CUSTOMER_NOTE)
            if (state.getBoolean(FIELD_IS_CONFIRMING_DISCARD)) {
                confirmDiscard()
            }
            state.getString(FIELD_NOTE_TEXT)?.let {
                addNote_editor.setText(it)
            }
        }

        if (orderId.isEmpty() || orderNumber.isEmpty()) {
            activity?.onBackPressed()
            return
        }

        if (presenter.hasBillingEmail(orderId)) {
            addNote_switch.setOnCheckedChangeListener { _, isChecked ->
                AnalyticsTracker.track(
                        ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
                        mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked)))

                val drawableId = if (isChecked) R.drawable.ic_note_public else R.drawable.ic_note_private
                addNote_icon.setImageDrawable(ContextCompat.getDrawable(activity!!, drawableId))
            }
        } else {
            addNote_switch.visibility = View.GONE
            addNote_switchDivider.visibility = View.GONE
            addNote_editDivider.visibility = View.GONE
        }

        if (savedInstanceState == null) {
            addNote_editor.requestFocus()
            ActivityUtils.showKeyboard(addNote_editor)
        }

        presenter.takeView(this)
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_orderstatus_ordernum, navArgs.orderNumber)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.menu_add, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_add -> {
                AnalyticsTracker.track(ADD_ORDER_NOTE_ADD_BUTTON_TAPPED)
                val noteText = getNoteText()
                if (noteText.isNotEmpty()) {
                    val isCustomerNote = addNote_switch.isChecked
                    if (presenter.pushOrderNote(orderId, noteText, isCustomerNote)) {
                        shouldShowDiscardDialog = false
                        activity?.onBackPressed()
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(FIELD_NOTE_TEXT, getNoteText())
        outState.putBoolean(FIELD_IS_CUSTOMER_NOTE, addNote_switch.isChecked)
        outState.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        super.onSaveInstanceState(outState)
    }

    override fun getNoteText() = addNote_editor?.text?.toString()?.trim() ?: ""

    /**
     * Prevent back press in the main activity if the user entered a note so we can confirm the discard
     */
    override fun onRequestAllowBackPress(): Boolean {
        return if (getNoteText().isNotEmpty() && shouldShowDiscardDialog) {
            confirmDiscard()
            false
        } else {
            true
        }
    }

    override fun confirmDiscard() {
        isConfirmingDiscard = true
        AlertDialog.Builder(activity)
                .setMessage(R.string.add_order_note_confirm_discard)
                .setCancelable(true)
                .setPositiveButton(R.string.discard) { _, _ ->
                    shouldShowDiscardDialog = false
                    activity?.onBackPressed()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    isConfirmingDiscard = false
                }
                .show()
    }

    override fun showAddOrderNoteSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_added).show()
    }

    override fun showAddOrderNoteErrorSnack() {
        uiMessageResolver.getSnack(R.string.add_order_note_error).show()
    }

    override fun showOfflineSnack() {
        uiMessageResolver.showOfflineSnack()
    }
}
