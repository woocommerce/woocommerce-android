package com.woocommerce.android.ui.payments.tracking

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface CardReaderTrackingInfoProvider {
    val trackingInfo: TrackingInfo
}

interface CardReaderTrackingInfoKeeper {
    fun setCountry(country: String?)
    fun setCurrency(currency: String)
    fun setPaymentMethodType(paymentMethodType: String?)
    fun setCardReaderModel(cardReaderModel: String?)
    fun setCardReaderBatteryLevel(batteryLevel: Float?)
}

@Singleton
class CardReaderTrackingInfoImpl @Inject constructor() : CardReaderTrackingInfoProvider, CardReaderTrackingInfoKeeper {
    @Volatile
    private var trackingInfoInternal = TrackingInfo()

    override val trackingInfo: TrackingInfo
        get() = trackingInfoInternal

    override fun setCountry(country: String?) {
        trackingInfoInternal = trackingInfoInternal.copy(country = country)
    }

    override fun setCurrency(currency: String) {
        trackingInfoInternal = trackingInfoInternal.copy(currency = currency)
    }

    override fun setPaymentMethodType(paymentMethodType: String?) {
        trackingInfoInternal = trackingInfoInternal.copy(paymentMethodType = paymentMethodType)
    }

    override fun setCardReaderModel(cardReaderModel: String?) {
        trackingInfoInternal = trackingInfoInternal.copy(cardReaderModel = cardReaderModel)
    }

    override fun setCardReaderBatteryLevel(batteryLevel: Float?) {
        trackingInfoInternal = trackingInfoInternal.copy(cardReaderBatteryLevel = batteryLevel)
    }
}

data class TrackingInfo(
    val country: String? = null,
    val currency: String? = null,
    val paymentMethodType: String? = null,
    val cardReaderModel: String? = null,
    val cardReaderBatteryLevel: Float? = null,
)

@InstallIn(SingletonComponent::class)
@Module
abstract class CardReaderTrackingModule {
    @Singleton
    @Binds
    abstract fun provideCardReaderTrackingInfoBuilder(impl: CardReaderTrackingInfoImpl): CardReaderTrackingInfoKeeper

    @Singleton
    @Binds
    abstract fun provideCardReaderTrackingInfoProvider(impl: CardReaderTrackingInfoImpl): CardReaderTrackingInfoProvider
}
