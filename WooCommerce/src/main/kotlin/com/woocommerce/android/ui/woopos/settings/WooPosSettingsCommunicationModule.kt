package com.woocommerce.android.ui.woopos.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
abstract class WooPosSettingsCommunicationModule {
    @Binds
    abstract fun bindChildToParentEventSender(
        communication: WooPosSettingsChildToParentCommunication
    ): WooPosSettingsChildToParentEventSender

    @Binds
    abstract fun bindChildToParentEventReceiver(
        communication: WooPosSettingsChildToParentCommunication
    ): WooPosSettingsChildToParentEventReceiver

    @Binds
    abstract fun bindParentToChildEventSender(
        communication: WooPosSettingsParentToChildCommunication
    ): WooPosSettingsParentToChildEventSender

    @Binds
    abstract fun bindParentToChildEventReceiver(
        communication: WooPosSettingsParentToChildCommunication
    ): WooPosSettingsParentToChildEventReceiver
}
