package com.woocommerce.android.ui.payments.cardreader.manuals

import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.ReaderType.BuildInReader.CotsDevice
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.Chipper2X
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.StripeM2
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.WisePade3
import com.woocommerce.android.ui.payments.cardreader.manuals.CardReaderManualsViewModel.ManualItem
import javax.inject.Inject

class CardReaderManualsSupportedReadersMapper @Inject constructor() {

    fun mapSupportedReadersToManualItems(
        cardReaderConfigForSupportedCountry: CardReaderConfigForSupportedCountry,
        clickListeners: Map<ReaderType, () -> Unit>
    ) = cardReaderConfigForSupportedCountry
        .supportedReaders.mapNotNull {
            when (it) {
                Chipper2X -> ManualItem(
                    icon = drawable.ic_chipper_reader,
                    label = string.card_reader_bbpos_manual_card_reader,
                    onManualClicked = clickListeners[it]!!
                )
                StripeM2 -> ManualItem(
                    icon = drawable.ic_m2_reader,
                    label = string.card_reader_m2_manual_card_reader,
                    onManualClicked = clickListeners[it]!!
                )
                WisePade3 -> ManualItem(
                    icon = drawable.ic_wisepad3_reader,
                    label = string.card_reader_wisepad_3_manual_card_reader,
                    onManualClicked = clickListeners[it]!!
                )
                CotsDevice -> null // This is built-in reader, we don't need to show it in the list
                else -> error("$it doesn't have a manual")
            }
        }
}
