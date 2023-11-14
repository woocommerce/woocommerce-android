package com.woocommerce.android.ui.login.storecreation.name

import android.Manifest
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.support.help.HelpOrigin.STORE_CREATION
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
@Suppress("UnusedPrivateMember")
class StoreNamePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    resourceProvider: ResourceProvider,
    private val newStore: NewStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val prefsWrapper: AppPrefsWrapper
) : ScopedViewModel(savedStateHandle) {
    override val _event = SingleLiveEvent<Event>()
    override val event: LiveData<Event> = _event

    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState("", false)
    )
    val viewState = _viewState.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_STEP,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME
            )
        )

        triggerEvent(CheckNotificationsPermission(::onCheckNotificationsPermissionResult))
        onStoreNameChanged(resourceProvider.getString(R.string.store_creation_store_name_default))
    }

    fun onCancelPressed() {
        analyticsTrackerWrapper.track(
            AnalyticsEvent.SITE_CREATION_DISMISSED,
            mapOf(
                AnalyticsTracker.KEY_STEP to AnalyticsTracker.VALUE_STEP_STORE_NAME,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_NATIVE,
                AnalyticsTracker.KEY_SOURCE to prefsWrapper.getStoreCreationSource(),
                AnalyticsTracker.KEY_IS_FREE_TRIAL to true
            )
        )
        triggerEvent(Exit)
    }

    fun onExitTriggered() {
        triggerEvent(Exit)
    }

    fun onHelpPressed() {
        triggerEvent(NavigateToHelpScreen(STORE_CREATION))
    }

    fun onStoreNameChanged(newName: String) {
        _viewState.update {
            ViewState(
                storeName = newName,
                isPermissionRationaleVisible = it.isPermissionRationaleVisible
            )
        }
    }

    fun onContinueClicked() {
        newStore.update(name = _viewState.value.storeName)
        triggerEvent(NavigateToStoreProfiler)
    }

    fun onPermissionRationaleDismissed() {
        setPermissionRationaleVisible(false)

        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.APP_PERMISSION_RATIONALE_DISMISSED,
                mapOf(AnalyticsTracker.KEY_TYPE to Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    fun onPermissionRationaleAccepted() {
        setPermissionRationaleVisible(false)
        triggerEvent(RequestNotificationsPermission(::onRequestNotificationsPermissionResult))

        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.APP_PERMISSION_RATIONALE_ACCEPTED,
                mapOf(AnalyticsTracker.KEY_TYPE to Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    private fun onCheckNotificationsPermissionResult(granted: Boolean, shouldShowRationale: Boolean) {
        if (!granted) {
            if (shouldShowRationale) {
                setPermissionRationaleVisible(true)
            } else {
                triggerEvent(RequestNotificationsPermission(::onRequestNotificationsPermissionResult))
            }
        }
    }

    private fun onRequestNotificationsPermissionResult(granted: Boolean) {
        if (granted) {
            onNotificationsPermissionGranted()
        } else {
            onNotificationsPermissionDenied()
        }
    }

    private fun onNotificationsPermissionGranted() {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.APP_PERMISSION_GRANTED,
                mapOf(AnalyticsTracker.KEY_TYPE to Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    private fun onNotificationsPermissionDenied() {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            analyticsTrackerWrapper.track(
                AnalyticsEvent.APP_PERMISSION_DENIED,
                mapOf(AnalyticsTracker.KEY_TYPE to Manifest.permission.POST_NOTIFICATIONS)
            )
        }
    }

    private fun setPermissionRationaleVisible(isVisible: Boolean) {
        _viewState.update {
            ViewState(
                storeName = it.storeName,
                isPermissionRationaleVisible = isVisible
            )
        }
    }

    object NavigateToStoreProfiler : Event()

    data class RequestNotificationsPermission(
        val onPermissionsRequestResult: (Boolean) -> Unit
    ) : Event()

    data class CheckNotificationsPermission(
        val onPermissionsCheckResult: (permissionGranted: Boolean, showRationale: Boolean) -> Unit
    ) : Event()

    @Parcelize
    data class ViewState(
        val storeName: String,
        val isPermissionRationaleVisible: Boolean
    ) : Parcelable
}
