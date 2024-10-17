//package com.woocommerce.android.ui.payments.cardreader.payment
//
//import androidx.lifecycle.SavedStateHandle
//import androidx.lifecycle.viewModelScope
//import com.woocommerce.android.cardreader.CardReaderManager
//import com.woocommerce.android.tools.SelectedSite
//import com.woocommerce.android.ui.orders.details.OrderDetailRepository
//import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
//import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
//import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
//import com.woocommerce.android.ui.payments.receipt.PaymentReceiptShare
//import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
//import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
//import com.woocommerce.android.util.CoroutineDispatchers
//import com.woocommerce.android.util.CurrencyFormatter
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.components.ViewModelComponent
//import org.wordpress.android.fluxc.store.WooCommerceStore
//
//@InstallIn(ViewModelComponent::class)
//@Module
//class CardReaderPaymentModule {
//    @Provides
//    fun provideCardReaderPaymentController(
//        savedState: SavedStateHandle,
//        cardReaderManager: CardReaderManager,
//        orderRepository: OrderDetailRepository,
//        selectedSite: SelectedSite,
//        paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker,
//        interacRefundableChecker: CardReaderInteracRefundableChecker,
//        tracker: PaymentsFlowTracker,
//        currencyFormatter: CurrencyFormatter,
//        errorMapper: CardReaderPaymentErrorMapper,
//        interacRefundErrorMapper: CardReaderInteracRefundErrorMapper,
//        wooStore: WooCommerceStore,
//        dispatchers: CoroutineDispatchers,
//        cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
//        cardReaderPaymentReaderTypeStateProvider: CardReaderPaymentReaderTypeStateProvider,
//        cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
//        paymentReceiptHelper: PaymentReceiptHelper,
//        cardReaderOnboardingChecker: CardReaderOnboardingChecker,
//        cardReaderConfigProvider: CardReaderCountryConfigProvider,
//        paymentReceiptShare: PaymentReceiptShare,
//        vm: CardReaderPaymentViewModel
//    ): CardReaderPaymentController {
//        return CardReaderPaymentController(
//            savedState = savedState,
//            cardReaderManager = cardReaderManager,
//            orderRepository = orderRepository,
//            selectedSite = selectedSite,
//            paymentCollectibilityChecker = paymentCollectibilityChecker,
//            interacRefundableChecker = interacRefundableChecker,
//            tracker = tracker,
//            currencyFormatter = currencyFormatter,
//            errorMapper = errorMapper,
//            interacRefundErrorMapper = interacRefundErrorMapper,
//            wooStore = wooStore,
//            dispatchers = dispatchers,
//            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
//            cardReaderPaymentReaderTypeStateProvider = cardReaderPaymentReaderTypeStateProvider,
//            cardReaderPaymentOrderHelper = cardReaderPaymentOrderHelper,
//            paymentReceiptHelper = paymentReceiptHelper,
//            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
//            cardReaderConfigProvider = cardReaderConfigProvider,
//            paymentReceiptShare = paymentReceiptShare,
//            scope = vm.viewModelScope,
//        )
//    }
//}