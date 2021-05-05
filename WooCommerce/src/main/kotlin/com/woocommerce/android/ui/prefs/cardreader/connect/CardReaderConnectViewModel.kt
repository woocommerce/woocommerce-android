package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.R.string
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

    sealed class ViewState(
        @StringRes val headerLabel: Int? = null,
        @DrawableRes val illustration: Int? = null,
        @StringRes val hintLabel: Int? = null,
        val primaryActionLabel: Int? = null,
        val secondaryActionLabel: Int? = null
    ) {
        open val onPrimaryActionClicked: (() -> Unit)? = null
        open val onSecondaryActionClicked: (() -> Unit)? = null

        data class ScanningState(override val onSecondaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = R.string.card_reader_connect_scanning_header,
            illustration = R.drawable.img_card_reader_scanning,
            hintLabel = R.string.card_reader_connect_scanning_hint,
            secondaryActionLabel = R.string.cancel
        )

        data class ReaderFoundState(
            override val onPrimaryActionClicked: (() -> Unit),
            override val onSecondaryActionClicked: (() -> Unit)
        ) : ViewState(
            headerLabel = R.string.card_reader_connect_scanning_header,
            illustration = R.drawable.img_card_reader,
            primaryActionLabel = R.string.card_reader_connect_to_reader,
            secondaryActionLabel = R.string.cancel
        )

        // TODO cardreader add multiple readers found state

        data class ConnectingState(override val onSecondaryActionClicked: (() -> Unit)) : ViewState(
            headerLabel = R.string.card_reader_connect_connecting_header,
            illustration = R.drawable.img_card_reader_connecting,
            hintLabel = R.string.card_reader_connect_connecting_hint,
            secondaryActionLabel = R.string.cancel
        )
    }

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<CardReaderConnectViewModel>
}
