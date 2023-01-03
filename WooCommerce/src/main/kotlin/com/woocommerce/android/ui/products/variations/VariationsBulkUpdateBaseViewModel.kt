package com.woocommerce.android.ui.products.variations

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.track
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base class for the variations bulk update view models.
 * This class handles the common logic for the variations bulk update feature:
 * * managing progress dialog visibility,
 * * sending analytics events
 * * and showing snackbars.
 *
 * The subclasses are responsible for handling the specific logic for each bulk update type, required by the abstract
 * methods of this class.
 */
abstract class VariationsBulkUpdateBaseViewModel(
    savedStateHandle: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
) : ScopedViewModel(savedStateHandle) {

    private val _isProgressDialogShown: MutableLiveData<Boolean> =
        savedStateHandle.getLiveData("progress_dialog_visibility", false)

    /**
     * Provides a live data object that can be observed for changes to the progress dialog visibility.
     */
    val isProgressDialogShown: LiveData<Boolean> = _isProgressDialogShown

    /**
     * Called from the UI layer when the user clicks on the "Done" button.
     */
    fun onDoneClicked() {
        _isProgressDialogShown.value = true

        track(getDoneClickedAnalyticsEvent())

        viewModelScope.launch {
            val result = performBulkUpdate()

            val snackText = if (result) {
                getSnackbarSuccessMessageTextRes()
            } else {
                R.string.variations_bulk_update_error
            }

            withContext(dispatchers.main) {
                _isProgressDialogShown.value = false
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(snackText))
                if (result) triggerEvent(MultiLiveEvent.Event.Exit)
            }
        }
    }

    /**
     * Provides the analytics event to be tracked when the user clicks on the "Done" button.
     */
    abstract fun getDoneClickedAnalyticsEvent(): AnalyticsEvent

    /**
     * Provides the text resource for the snackbar message to be shown when the bulk update is successful.
     */
    @StringRes
    abstract fun getSnackbarSuccessMessageTextRes(): Int

    /**
     * Performs the bulk update of the variations.
     */
    abstract suspend fun performBulkUpdate(): Boolean
}
