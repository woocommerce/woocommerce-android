package com.woocommerce.android.ui.woopos.settings

import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@ActivityRetainedScoped
class WooPosSettingsChildToParentCommunication @Inject constructor() :
    WooPosSettingsChildToParentEventReceiver, WooPosSettingsChildToParentEventSender {
    private val _events = MutableSharedFlow<SettingsChildToParentEvent>()
    override val events = _events.asSharedFlow()

    override suspend fun sendToParent(event: SettingsChildToParentEvent) {
        _events.emit(event)
    }
}

@ActivityRetainedScoped
class WooPosSettingsParentToChildCommunication @Inject constructor() :
    WooPosSettingsParentToChildEventReceiver, WooPosSettingsParentToChildEventSender {
    private val _events = MutableSharedFlow<SettingsParentToChildEvent>()
    override val events = _events.asSharedFlow()

    override suspend fun sendToChild(event: SettingsParentToChildEvent) {
        _events.emit(event)
    }
}

sealed class SettingsChildToParentEvent {
    data class ShowSyncErrorDialog(val errorMessage: String) : SettingsChildToParentEvent()
}

sealed class SettingsParentToChildEvent {
    data object RetrySyncRequested : SettingsParentToChildEvent()
}

interface WooPosSettingsChildToParentEventReceiver {
    val events: Flow<SettingsChildToParentEvent>
}

interface WooPosSettingsChildToParentEventSender {
    suspend fun sendToParent(event: SettingsChildToParentEvent)
}

interface WooPosSettingsParentToChildEventReceiver {
    val events: Flow<SettingsParentToChildEvent>
}

interface WooPosSettingsParentToChildEventSender {
    suspend fun sendToChild(event: SettingsParentToChildEvent)
}
