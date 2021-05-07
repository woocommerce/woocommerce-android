package com.woocommerce.android.ui.prefs.cardreader.connect

import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.DaggerScopedViewModel
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog.T

class CardReaderConnectViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val appLogWrapper: AppLogWrapper
) : DaggerScopedViewModel(savedState, dispatchers) {
    fun foo() {
        appLogWrapper.d(T.MAIN, "Connect foo() invoked")
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<CardReaderConnectViewModel>
}
