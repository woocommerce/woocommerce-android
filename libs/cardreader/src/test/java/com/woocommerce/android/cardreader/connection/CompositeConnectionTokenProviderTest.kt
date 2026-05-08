package com.woocommerce.android.cardreader.connection

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class CompositeConnectionTokenProviderTest {
    private val defaultDelegate: ConnectionTokenProvider = mock()
    private val remoteDelegate: RemoteTokenChannelProvider = mock()
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
        verifyNoInteractions(remoteDelegate)
    }

    @Test
    fun `given useRemote was called, when fetchConnectionToken is called, then remote delegate receives it`() {
        // GIVEN
        sut.useRemote(remoteDelegate)

        // WHEN
        sut.fetchConnectionToken(callback)

        // THEN
        verify(remoteDelegate).fetchConnectionToken(callback)
        verifyNoInteractions(defaultDelegate)
    }

    @Test
    fun `given remote was active, when useDefault is called, then default delegate receives fetch`() {
        // GIVEN
        sut.useRemote(remoteDelegate)
        sut.useDefault()

        // WHEN
        sut.fetchConnectionToken(callback)

        // THEN
        verify(defaultDelegate).fetchConnectionToken(callback)
        verifyNoInteractions(remoteDelegate)
    }
}
