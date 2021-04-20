package com.woocommerce.android.ui.orders.cardreader

import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T

class CardReaderPaymentViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val logger: AppLogWrapper
) : ScopedViewModel(savedState, dispatchers) {
    fun foo() {
        logger.d(T.MAIN, "Testing foo()")
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<CardReaderPaymentViewModel>
}
