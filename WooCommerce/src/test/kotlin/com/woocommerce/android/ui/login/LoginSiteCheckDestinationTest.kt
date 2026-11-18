package com.woocommerce.android.ui.login

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class LoginSiteCheckDestinationTest {
    @Test
    fun `given suggested login through WPCom, when resolving site check, then go to WPCom email`() {
        val destination = resolveSiteCheckDestination(isSuggestedLogin = true, logsInThroughWPCom = true)

        assertThat(destination).isEqualTo(SiteCheckDestination.WPCOM_EMAIL)
    }

    @Test
    fun `given suggested login not through WPCom, when resolving site check, then go to empty site address`() {
        val destination = resolveSiteCheckDestination(isSuggestedLogin = true, logsInThroughWPCom = false)

        assertThat(destination).isEqualTo(SiteCheckDestination.EMPTY_SITE_ADDRESS)
    }

    @Test
    fun `given normal login through WPCom, when resolving site check, then go to WPCom email`() {
        val destination = resolveSiteCheckDestination(isSuggestedLogin = false, logsInThroughWPCom = true)

        assertThat(destination).isEqualTo(SiteCheckDestination.WPCOM_EMAIL)
    }

    @Test
    fun `given normal login not through WPCom, when resolving site check, then go to site credentials`() {
        val destination = resolveSiteCheckDestination(isSuggestedLogin = false, logsInThroughWPCom = false)

        assertThat(destination).isEqualTo(SiteCheckDestination.SITE_CREDENTIALS)
    }

    @Test
    fun `given WPCom auth required and normal login, when checking fallback, then hide it`() {
        val showFallback = shouldShowSiteCredentialsFallback(shouldUseWPComAuth = true, isSuggestedLogin = false)

        assertThat(showFallback).isFalse()
    }

    @Test
    fun `given WPCom auth not required and normal login, when checking fallback, then show it`() {
        val showFallback = shouldShowSiteCredentialsFallback(shouldUseWPComAuth = false, isSuggestedLogin = false)

        assertThat(showFallback).isTrue()
    }

    @Test
    fun `given WPCom auth unknown and normal login, when checking fallback, then hide it`() {
        val showFallback = shouldShowSiteCredentialsFallback(shouldUseWPComAuth = null, isSuggestedLogin = false)

        assertThat(showFallback).isFalse()
    }

    @Test
    fun `given WPCom auth required and suggested login, when checking fallback, then hide it`() {
        val showFallback = shouldShowSiteCredentialsFallback(shouldUseWPComAuth = true, isSuggestedLogin = true)

        assertThat(showFallback).isFalse()
    }

    @Test
    fun `given WPCom auth not required and suggested login, when checking fallback, then hide it`() {
        val showFallback = shouldShowSiteCredentialsFallback(shouldUseWPComAuth = false, isSuggestedLogin = true)

        assertThat(showFallback).isFalse()
    }

    @Test
    fun `given WPCom auth unknown and suggested login, when checking fallback, then hide it`() {
        val showFallback = shouldShowSiteCredentialsFallback(shouldUseWPComAuth = null, isSuggestedLogin = true)

        assertThat(showFallback).isFalse()
    }
}
