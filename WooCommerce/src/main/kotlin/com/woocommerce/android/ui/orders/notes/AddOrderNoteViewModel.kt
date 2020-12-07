package com.woocommerce.android.ui.orders.notes

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier

class AddOrderNoteViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispathers: CoroutineDispatchers,
    private val resourceProvider: ResourceProvider,
    private val networkStatus: NetworkStatus,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispathers) {

    val addOrderNoteViewStateData = LiveDataDelegate(savedState, ViewState())
    private var addOrderNoteViewState by addOrderNoteViewStateData

    private val navArgs: AddOrderNoteFragmentArgs by savedState.navArgs()

    private val orderId: OrderIdentifier
        get() = navArgs.orderId

    private val orderNumber: String
        get() = navArgs.orderNumber

    val screenTitle: String
        get() = resourceProvider.getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

    init {
        if (orderId.isEmpty() || orderNumber.isEmpty()) {
            triggerEvent(Exit)
        }
        checkIfHasBillingMail()
    }

    fun onOrderTextEntered(text: String) {
        val draftNote = addOrderNoteViewState.draftNote.copy(note = text)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote, canAddNote = text.isNotBlank())
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
        AnalyticsTracker.track(ORDER_NOTE_ADD, mapOf(AnalyticsTracker.KEY_PARENT_ID to order.remoteId))

        addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = true)

        val note = addOrderNoteViewState.draftNote
        launch {
            if (orderDetailRepository.addOrderNote(order.identifier, order.remoteId, note)) {
                addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = false)
                triggerEvent(ShowSnackbar(R.string.add_order_note_added))
                triggerEvent(ExitWithResult(note))
            } else {
                addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = false)
                triggerEvent(ShowSnackbar(R.string.add_order_note_error))
            }
        }
    }

    fun onBackPressed() {
        if (addOrderNoteViewState.draftNote.note.trim().isNotEmpty()) {
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
        } else {
            triggerEvent(Exit)
        }
    }

    @Parcelize
    data class ViewState(
        val draftNote: OrderNote = OrderNote(note = "", isCustomerNote = false),
        val canAddNote: Boolean = false,
        val showCustomerNoteSwitch: Boolean = false,
        val isProgressDialogShown: Boolean = false
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddOrderNoteViewModel>
}