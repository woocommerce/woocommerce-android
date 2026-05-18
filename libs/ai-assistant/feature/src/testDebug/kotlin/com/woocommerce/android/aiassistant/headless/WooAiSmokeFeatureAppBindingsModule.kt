package com.woocommerce.android.aiassistant.headless

import android.content.Context
import com.automattic.eventhorizon.Trackable
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WooAiSmokeFeatureAppBindingsModule {
    @Provides
    fun provideUnqualifiedContext(@ApplicationContext context: Context): Context = context

    @Provides
    fun provideBackgroundDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideUserAgent(context: Context): UserAgent = UserAgent(context, USER_AGENT_APP_NAME)

    @Provides
    fun provideAppSecrets(): AppSecrets = AppSecrets("", "")

    @Provides
    fun provideAssistantTelemetryTracker(): AssistantTelemetryTracker =
        object : AssistantTelemetryTracker {
            override fun track(event: Trackable) = Unit
        }

    @Provides
    @Singleton
    fun provideApplicationPasswordsConfiguration(): ApplicationPasswordsConfiguration =
        object : ApplicationPasswordsConfiguration {
            override val applicationName: String = "woocommerce-android-ai-smoke"

            override fun isEnabledForDirectAccess(): Boolean = true

            override suspend fun isEnabledForJetpackAccess(): Boolean = false
        }

    private const val USER_AGENT_APP_NAME = "wc-android"
}
