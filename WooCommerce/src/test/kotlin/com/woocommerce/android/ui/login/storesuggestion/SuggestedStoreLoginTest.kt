package com.woocommerce.android.ui.login.storesuggestion

import com.woocommerce.android.AppPrefsWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SuggestedStoreLoginTest {
    private var storedUrl = ""
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val sut = SuggestedStoreLogin(appPrefsWrapper)

    @Before
    fun setUp() {
        whenever(appPrefsWrapper.suggestedStoreLoginUrl).thenAnswer { storedUrl }
        doAnswer { storedUrl = it.getArgument(0) }
            .whenever(appPrefsWrapper).suggestedStoreLoginUrl = any()
    }

    @Test
    fun `given login started, when onAddressSubmitted with same host, then returns true`() {
        sut.start(STORE_HOST)

        assertThat(sut.onAddressSubmitted("https://$STORE_HOST")).isTrue()
    }

    @Test
    fun `given login started, when onAddressSubmitted with same host, then login stays active`() {
        sut.start(STORE_HOST)

        sut.onAddressSubmitted("https://$STORE_HOST")

        assertThat(sut.isActiveFor(STORE_HOST)).isTrue()
    }

    @Test
    fun `given login started, when isActiveFor with www prefix, then returns true`() {
        sut.start(STORE_HOST)

        assertThat(sut.isActiveFor("www.$STORE_HOST")).isTrue()
    }

    @Test
    fun `given login started with www prefix, when isActiveFor without it, then returns true`() {
        sut.start("www.$STORE_HOST")

        assertThat(sut.isActiveFor(STORE_HOST)).isTrue()
    }

    @Test
    fun `given login started, when isActiveFor with scheme, then returns true`() {
        sut.start(STORE_HOST)

        assertThat(sut.isActiveFor("http://$STORE_HOST")).isTrue()
    }

    @Test
    fun `given login started, when onAddressSubmitted with different host, then returns false`() {
        sut.start(STORE_HOST)

        assertThat(sut.onAddressSubmitted(OTHER_HOST)).isFalse()
    }

    @Test
    fun `given login started, when onAddressSubmitted with different host, then login stays ended`() {
        sut.start(STORE_HOST)

        sut.onAddressSubmitted(OTHER_HOST)

        assertThat(sut.onAddressSubmitted(STORE_HOST)).isFalse()
    }

    @Test
    fun `given login started, when isActiveFor with different host, then login is not ended`() {
        sut.start(STORE_HOST)

        val isActiveForOtherHost = sut.isActiveFor(OTHER_HOST)

        assertThat(isActiveForOtherHost).isFalse()
        assertThat(sut.isActiveFor(STORE_HOST)).isTrue()
    }

    @Test
    fun `given nothing active, when isActiveFor, then returns false`() {
        assertThat(sut.isActiveFor(STORE_HOST)).isFalse()
    }

    @Test
    fun `given nothing active, when onAddressSubmitted, then returns false`() {
        assertThat(sut.onAddressSubmitted(STORE_HOST)).isFalse()
    }

    @Test
    fun `given login started, when end, then login is not active`() {
        sut.start(STORE_HOST)

        sut.end()

        assertThat(sut.isActiveFor(STORE_HOST)).isFalse()
    }

    @Test
    fun `given login started, when followRedirect to a new host, then returns true`() {
        sut.start(STORE_HOST)

        assertThat(sut.followRedirect(submittedUrl = STORE_HOST, finalUrl = OTHER_HOST)).isTrue()
    }

    @Test
    fun `given login started, when followRedirect to a new host, then only the target is active`() {
        sut.start(STORE_HOST)

        sut.followRedirect(submittedUrl = STORE_HOST, finalUrl = OTHER_HOST)

        assertThat(sut.isActiveFor(STORE_HOST)).isFalse()
        assertThat(sut.isActiveFor(OTHER_HOST)).isTrue()
    }

    @Test
    fun `given login started, when followRedirect differs only by www, then returns false`() {
        sut.start(STORE_HOST)

        assertThat(sut.followRedirect(submittedUrl = STORE_HOST, finalUrl = "www.$STORE_HOST")).isFalse()
    }

    @Test
    fun `given login started, when followRedirect differs only by www, then the original stays stored`() {
        sut.start(STORE_HOST)

        sut.followRedirect(submittedUrl = STORE_HOST, finalUrl = "www.$STORE_HOST")

        assertThat(storedUrl).isEqualTo(STORE_HOST)
        assertThat(sut.isActiveFor(STORE_HOST)).isTrue()
    }

    private companion object {
        const val STORE_HOST = "store.com"
        const val OTHER_HOST = "other.com"
    }
}
