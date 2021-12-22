package com.woocommerce.android.ui.orders.notes

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

@HiltViewModel
class AddOrderNoteViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val networkStatus: NetworkStatus,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    val addOrderNoteViewStateData = LiveDataDelegate(savedState, ViewState())
    private var addOrderNoteViewState by addOrderNoteViewStateData

    private val navArgs: AddOrderNoteFragmentArgs by savedState.navArgs()

    private val orderId: OrderIdentifier
        get() = navArgs.orderId

    private val orderNumber: String
        get() = navArgs.orderNumber

    val screenTitle: String
        get() = resourceProvider.getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

    val shouldShowAddButton: Boolean
        get() = addOrderNoteViewState.canAddNote

    init {
        if (orderId.isEmpty() || orderNumber.isEmpty()) {
            triggerEvent(Exit)
        }
        checkIfHasBillingMail()
    }

    fun onOrderTextEntered(text: String) {
        val draftNote = addOrderNoteViewState.draftNote.copy(note = text)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote)
    }

    fun onIsCustomerCheckboxChanged(isChecked: Boolean) {
        AnalyticsTracker.track(
            ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
            mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked))
        )
        val draftNote = addOrderNoteViewState.draftNote.copy(isCustomerNote = isChecked)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote)
    }

    private fun checkIfHasBillingMail() {
        val email = orderDetailRepository.getOrder(orderId)?.billingAddress?.email
        addOrderNoteViewState = addOrderNoteViewState.copy(showCustomerNoteSwitch = email?.isNotEmpty() == true)
    }

    fun pushOrderNote() {
        if (addOrderNoteViewState.draftNote.note.isBlank()) return

        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            return
        }

        val order = orderDetailRepository.getOrder(orderId)
        if (order == null) {
            triggerEvent(ShowSnackbar(R.string.add_order_note_error))
            return
        }
        AnalyticsTracker.track(ORDER_NOTE_ADD, mapOf(AnalyticsTracker.KEY_PARENT_ID to order.id))

        addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = true)

        val note = addOrderNoteViewState.draftNote
        launch {
            val onOrderChanged = orderDetailRepository.addOrderNote(order.identifier, order.id, note)
            if (!onOrderChanged.isError) {
                AnalyticsTracker.track(Stat.ORDER_NOTE_ADD_SUCCESS)
                addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = false)
                triggerEvent(ShowSnackbar(R.string.add_order_note_added))
                triggerEvent(ExitWithResult(note))
            } else {
                AnalyticsTracker.track(
                    Stat.ORDER_NOTE_ADD_FAILED,
                    prepareTracksEventsDetails(onOrderChanged)
                )
                addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = false)
                triggerEvent(ShowSnackbar(R.string.add_order_note_error))
            }
        }
    }

    private fun prepareTracksEventsDetails(event: OnOrderChanged) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to event.error.message
    )

    fun onBackPressed() {
        if (addOrderNoteViewState.draftNote.note.trim().isNotEmpty()) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(Exit)
                    }
                )
            )
        } else {
            triggerEvent(Exit)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    @Parcelize
    data class ViewState(
        val draftNote: OrderNote = OrderNote(note = "", isCustomerNote = false),
        val showCustomerNoteSwitch: Boolean = false,
        val isProgressDialogShown: Boolean = false
    ) : Parcelable {
        val canAddNote: Boolean
            get() = draftNote.note.isNotBlank()
    }
}
