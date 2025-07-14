package com.woocommerce.android.ui.orders.notes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class AddOrderNoteViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val networkStatus: NetworkStatus,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val addOrderNoteViewStateData = LiveDataDelegate(savedState, ViewState())
    private var addOrderNoteViewState by addOrderNoteViewStateData

    private val navArgs: AddOrderNoteFragmentArgs by savedState.navArgs()

    private val orderId: Long
        get() = navArgs.orderId

    private val orderNumber: String
        get() = navArgs.orderNumber

    val screenTitle: String
        get() = resourceProvider.getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

    val shouldShowAddButton: Boolean
        get() = addOrderNoteViewState.canAddNote

    init {
        checkIfHasBillingMail()
    }

    fun onOrderTextEntered(text: String) {
        val draftNote = addOrderNoteViewState.draftNote.copy(note = text)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote)
    }

    fun onIsCustomerCheckboxChanged(isChecked: Boolean) {
        AnalyticsTracker.track(
            AnalyticsEvent.ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
            mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked))
        )
        val draftNote = addOrderNoteViewState.draftNote.copy(isCustomerNote = isChecked)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote)
    }

    private fun checkIfHasBillingMail() {
        launch {
            val email = orderDetailRepository.getOrderById(orderId)?.billingAddress?.email
            addOrderNoteViewState = addOrderNoteViewState.copy(showCustomerNoteSwitch = email?.isNotEmpty() == true)
        }
    }

    fun pushOrderNote() {
        if (addOrderNoteViewState.draftNote.note.isBlank()) return

        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            return
        }

        launch {
            val order = orderDetailRepository.getOrderById(orderId)
            if (order == null) {
                triggerEvent(ShowSnackbar(R.string.add_order_note_error))
                return@launch
            }
            // Fix: add type of the note. Look at documentation for more details
            AnalyticsTracker.track(AnalyticsEvent.ORDER_NOTE_ADD, mapOf(AnalyticsTracker.KEY_PARENT_ID to order.id))

            addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = true)

            val note = addOrderNoteViewState.draftNote

            orderDetailRepository.addOrderNote(order.id, note)
                .fold(
                    onSuccess = {
                        AnalyticsTracker.track(AnalyticsEvent.ORDER_NOTE_ADD_SUCCESS)
                        addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = false)
                        triggerEvent(ShowSnackbar(R.string.add_order_note_added))
                        triggerEvent(ExitWithResult(note))
                    },
                    onFailure = {
                        AnalyticsTracker.track(
                            AnalyticsEvent.ORDER_NOTE_ADD_FAILED,
                            prepareTracksEventsDetails((it as WooException))
                        )
                        addOrderNoteViewState = addOrderNoteViewState.copy(isProgressDialogShown = false)
                        triggerEvent(ShowSnackbar(R.string.add_order_note_error))
                    }
                )
        }
    }

    private fun prepareTracksEventsDetails(exception: WooException) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to exception.error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to exception.error.message
    )

    fun onBackPressed() {
        if (addOrderNoteViewState.draftNote.note.trim().isNotEmpty()) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = { _, _ ->
                        triggerEvent(Exit)
                    }
                )
            )
        } else {
            triggerEvent(Exit)
        }
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
