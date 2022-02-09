package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderManagerFactory
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.prefs.cardreader.onboarding.toInPersonPaymentsPluginType
import com.woocommerce.android.util.CapturePaymentResponseMapper
import com.woocommerce.android.util.WooLog
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Singleton
    fun provideCardReaderManager(
        application: Application,
        cardReaderStore: CardReaderStore,
        logWrapper: LogWrapper
    ) = CardReaderManagerFactory.createCardReaderManager(application, cardReaderStore, logWrapper)

    @Provides
    fun provideInPersonPaymentsStore(
        selectedSite: SelectedSite,
        inPersonPaymentsStore: WCInPersonPaymentsStore,
        responseMapper: CapturePaymentResponseMapper,
        appPrefs: AppPrefs
    ) = object : CardReaderStore {
        override suspend fun fetchCustomerIdByOrderId(orderId: Long): String? {
            return inPersonPaymentsStore.createCustomerByOrderId(
                appPrefs.getPaymentPluginType(
                    selectedSite.get().id,
                    selectedSite.get().siteId,
                    selectedSite.get().selfHostedSiteId
                ).toInPersonPaymentsPluginType(),
                selectedSite.get(),
                orderId
            ).model?.customerId
        }

        override suspend fun fetchConnectionToken(): String {
            val result = inPersonPaymentsStore.fetchConnectionToken(
                appPrefs.getPaymentPluginType(
                    selectedSite.get().id,
                    selectedSite.get().siteId,
                    selectedSite.get().selfHostedSiteId
                ).toInPersonPaymentsPluginType(),
                selectedSite.get()
            )
            return result.model?.token.orEmpty()
        }

        override suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse {
            val response = inPersonPaymentsStore.capturePayment(
                appPrefs.getPaymentPluginType(
                    selectedSite.get().id,
                    selectedSite.get().siteId,
                    selectedSite.get().selfHostedSiteId
                ).toInPersonPaymentsPluginType(),
                selectedSite.get(),
                paymentId,
                orderId
            )
            return responseMapper.mapResponse(response)
        }
    }

    @Provides
    fun provideLogWrapper() = object : LogWrapper {
        private val TAG = WooLog.T.CARD_READER

        override fun w(tag: String, message: String) {
            WooLog.w(TAG, "$tag: $message")
        }

        override fun d(tag: String, message: String) {
            WooLog.d(TAG, "$tag: $message")
        }

        override fun e(tag: String, message: String) {
            WooLog.e(TAG, "$tag: $message")
        }
    }

    @Provides
    @Reusable
    fun provideCardReaderConfigFactory() = CardReaderConfigFactory()
}
