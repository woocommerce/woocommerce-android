package com.woocommerce.android.ui.woopos.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosConnectionTypeProviderTest {
    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities

    private lateinit var sut: WooPosConnectionTypeProvider

    @Before
    fun setup() {
        context = mock()
        connectivityManager = mock()
        network = mock()
        networkCapabilities = mock()

        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)

        sut = WooPosConnectionTypeProvider(context)
    }

    @Test
    fun `when connected via WiFi, then returns WIFI`() {
        // GIVEN
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(false)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.WIFI)
    }

    @Test
    fun `when connected via cellular, then returns CELLULAR`() {
        // GIVEN
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.CELLULAR)
    }

    @Test
    fun `when connected via both WiFi and cellular, then returns WIFI as priority`() {
        // GIVEN
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.WIFI)
    }

    @Test
    fun `when no active network, then returns UNKNOWN`() {
        // GIVEN
        whenever(connectivityManager.activeNetwork).thenReturn(null)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.UNKNOWN)
    }

    @Test
    fun `when network capabilities is null, then returns UNKNOWN`() {
        // GIVEN
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(null)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.UNKNOWN)
    }

    @Test
    fun `when connectivity manager is null, then returns UNKNOWN`() {
        // GIVEN
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null)
        sut = WooPosConnectionTypeProvider(context)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.UNKNOWN)
    }

    @Test
    fun `when connectivity manager is not ConnectivityManager type, then returns UNKNOWN`() {
        // GIVEN
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn("NotConnectivityManager")
        sut = WooPosConnectionTypeProvider(context)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.UNKNOWN)
    }

    @Test
    fun `when connected via other transport type, then returns UNKNOWN`() {
        // GIVEN
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network)).thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(false)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(false)
        whenever(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true)

        // WHEN
        val result = sut.getConnectionType()

        // THEN
        assertThat(result).isEqualTo(ConnectionType.UNKNOWN)
    }
}
