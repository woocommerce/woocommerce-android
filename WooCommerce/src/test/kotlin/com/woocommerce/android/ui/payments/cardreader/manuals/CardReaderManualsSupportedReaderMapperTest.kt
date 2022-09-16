package com.woocommerce.android.ui.payments.cardreader.manuals

import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.connection.SpecificReader
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.ui.payments.cardreader.manuals.CardReaderManualsViewModel.ManualItem
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CardReaderManualsSupportedReaderMapperTest : BaseUnitTest() {
    private lateinit var manualsMapper: CardReaderManualsSupportedReadersMapper

    @Before
    fun setup() {
        manualsMapper = CardReaderManualsSupportedReadersMapper()
    }

    @Test
    fun `given US store, when screen shown, US readers are displayed`() {

        val supportedCountry = CardReaderConfigForUSA
        val expectedManualItems = listOf(
            ManualItem(
                icon = drawable.ic_chipper_reader,
                label = string.card_reader_bbpos_manual_card_reader,
                onManualClicked = ::onManualClicked
            ),
            ManualItem(
                icon = drawable.ic_m2_reader,
                label = string.card_reader_m2_manual_card_reader,
                onManualClicked = ::onManualClicked
            )
        )

        val actualManualItems = manualsMapper.mapSupportedReadersToManualItems(
            supportedCountry,
            mapOf(
                SpecificReader.Chipper2X to ::onManualClicked,
                SpecificReader.StripeM2 to ::onManualClicked,
            )
        )

        assertThat(expectedManualItems).isEqualTo(actualManualItems)
    }

    @Test
    fun `give canadian store, when screen shown, CA readers are displayed`() {
        val supportedCountry = CardReaderConfigForCanada
        val expectedManuals = listOf(
            ManualItem(
                icon = drawable.ic_wisepad3_reader,
                label = string.card_reader_wisepad_3_manual_card_reader,
                onManualClicked = ::onManualClicked
            )
        )
        val actualManualItems = manualsMapper.mapSupportedReadersToManualItems(
            supportedCountry,
            mapOf(
                SpecificReader.WisePade3 to ::onManualClicked
            )
        )

        assertThat(expectedManuals).isEqualTo(actualManualItems)
    }
}

private fun onManualClicked() {
    // dummy function
}
