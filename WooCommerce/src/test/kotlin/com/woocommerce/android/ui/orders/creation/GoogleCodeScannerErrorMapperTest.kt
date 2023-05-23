package com.woocommerce.android.ui.orders.creation

import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.MlKitException.ABORTED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_CANCELLED
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GoogleCodeScannerErrorMapperTest : BaseUnitTest() {
    private lateinit var mapper: GoogleCodeScannerErrorMapper

    private val mlKitException =  mock<MlKitException>().also {
        whenever(it.errorCode).thenReturn(CODE_SCANNER_CANCELLED)
    }

    @Before
    fun setup() {
        mapper = GoogleCodeScannerErrorMapper()
    }

    @Test
    fun `when ABORTED exception thrown, then Aborted type returned`() {
        whenever(mlKitException.errorCode).thenReturn(ABORTED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.Aborted
        )
    }
}
