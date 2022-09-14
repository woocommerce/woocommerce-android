package com.woocommerce.android.ui.payments.cardreader.manuals

import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any


@ExperimentalCoroutinesApi
class CardReaderManualsSupportedReaderMapperTest: BaseUnitTest() {
    private lateinit var manualsMapper: CardReaderManualsSupportedReadersMapper

    @Before
    fun setup() {
        manualsMapper = CardReaderManualsSupportedReadersMapper()
    }

    @Test
    fun `given US store, when screen shown, US readers are displayed`() {


    }
}
