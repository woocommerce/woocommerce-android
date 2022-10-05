package com.woocommerce.android.ui.payments.cardreader.manuals

import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.connection.SpecificReader
import com.woocommerce.android.cardreader.connection.SpecificReader.Chipper2X
import com.woocommerce.android.cardreader.connection.SpecificReader.StripeM2
import com.woocommerce.android.cardreader.connection.SpecificReader.WisePade3
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.ui.payments.cardreader.manuals.CardReaderManualsViewModel.ManualItem
import javax.inject.Inject

class CardReaderManualsSupportedReadersMapper @Inject constructor() {

    fun mapSupportedReadersToManualItems(
        cardReaderConfigForSupportedCountry: CardReaderConfigForSupportedCountry,
        clickListeners: Map<SpecificReader, () -> Unit>
    ) = cardReaderConfigForSupportedCountry
        .supportedReaders.map {
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
                else -> error("$it doesn't have a manual")
            }
        }
}
