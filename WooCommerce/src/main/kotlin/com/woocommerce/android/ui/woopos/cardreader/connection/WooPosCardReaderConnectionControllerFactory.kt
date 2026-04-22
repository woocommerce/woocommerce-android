package com.woocommerce.android.ui.woopos.cardreader.connection

import android.content.Context
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.PointOfSaleMode
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.LocationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class WooPosCardReaderConnectionControllerFactory @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val locationRepository: CardReaderLocationRepository,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val developerOptionsRepository: DeveloperOptionsRepository,
    private val dispatchers: CoroutineDispatchers,
    @ApplicationContext private val context: Context,
    private val locationUtils: LocationUtils,
    private val logger: WooPosLogWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    @PointOfSaleMode private val tracker: PaymentsFlowTracker,
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper,
    private val onboardingErrorMapper: WooPosOnboardingErrorMapper,
) {
    fun create(scope: CoroutineScope): WooPosCardReaderConnectionController {
        return WooPosCardReaderConnectionController(
            cardReaderManager = cardReaderManager,
            locationRepository = locationRepository,
            cardReaderOnboardingChecker = cardReaderOnboardingChecker,
            developerOptionsRepository = developerOptionsRepository,
            dispatchers = dispatchers,
            scope = scope,
            context = context,
            locationUtils = locationUtils,
            logger = logger,
            appPrefsWrapper = appPrefsWrapper,
            tracker = tracker,
            cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
            onboardingErrorMapper = onboardingErrorMapper,
        )
    }
}
