package com.woocommerce.android.ui.login

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ConnectSiteInfoTest {
    @Test
    fun `when site is commerce garden, then should use WPCom auth`() {
        val siteInfo = LoginActivity.ConnectSiteInfo(
            isWPCom = false,
            isCommerceGarden = true,
            isJetpackConnected = false,
            isJetpackActive = false
        )

        assertThat(siteInfo.shouldUseWPComAuth).isTrue
    }

    @Test
    fun `when site is WPCom, then should use WPCom auth`() {
        val siteInfo = LoginActivity.ConnectSiteInfo(
            isWPCom = true,
            isCommerceGarden = false,
            isJetpackConnected = false,
            isJetpackActive = false
        )

        assertThat(siteInfo.shouldUseWPComAuth).isTrue
    }

    @Test
    fun `when site is self-hosted Jetpack, then should not use WPCom auth`() {
        val siteInfo = LoginActivity.ConnectSiteInfo(
            isWPCom = false,
            isCommerceGarden = false,
            isJetpackConnected = true,
            isJetpackActive = true
        )

        assertThat(siteInfo.shouldUseWPComAuth).isFalse
    }

    @Test
    fun `when site is commerce garden, then site credentials fallback is hidden`() {
        val siteInfo = LoginActivity.ConnectSiteInfo(
            isWPCom = false,
            isCommerceGarden = true,
            isJetpackConnected = false,
            isJetpackActive = false
        )

        val showSiteCredentialsFallback = !siteInfo.shouldUseWPComAuth

        assertThat(showSiteCredentialsFallback).isFalse
    }

    @Test
    fun `when site is self-hosted Jetpack, then site credentials fallback is shown`() {
        val siteInfo = LoginActivity.ConnectSiteInfo(
            isWPCom = false,
            isCommerceGarden = false,
            isJetpackConnected = true,
            isJetpackActive = true
        )

        val showSiteCredentialsFallback = !siteInfo.shouldUseWPComAuth

        assertThat(showSiteCredentialsFallback).isTrue
    }
}
