package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class CompositeConnectionTokenProviderTest {
    private val defaultDelegate: ConnectionTokenProvider = mock()
    private val altDelegate: ConnectionTokenProvider = mock()
    private val callback: ConnectionTokenCallback = mock()

    private val sut = CompositeConnectionTokenProvider(defaultDelegate)

    @Test
    fun `given default mode, when fetchConnectionToken is called, then default delegate receives it`() {
        // GIVEN
        // new composite with default delegate

        // WHEN
        sut.fetchConnectionToken(callback)

        // THEN
        verify(defaultDelegate).fetchConnectionToken(callback)
        verifyNoInteractions(altDelegate)
    }

    @Test
    fun `given use(alt) was called, when fetchConnectionToken is called, then alt delegate receives it`() {
        // GIVEN
        sut.use(altDelegate)

        // WHEN
        sut.fetchConnectionToken(callback)

        // THEN
        verify(altDelegate).fetchConnectionToken(callback)
        verifyNoInteractions(defaultDelegate)
    }

    @Test
    fun `given alt was active, when useDefault is called, then default delegate receives fetch`() {
        // GIVEN
        sut.use(altDelegate)
        sut.useDefault()

        // WHEN
        sut.fetchConnectionToken(callback)

        // THEN
        verify(defaultDelegate).fetchConnectionToken(callback)
        verifyNoInteractions(altDelegate)
    }
}
