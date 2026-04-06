package org.wordpress.android.fluxc.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.SiteEntity

class SiteMapperTest {
    private val mapper = SiteMapper()

    // region toEntity

    @Test
    fun `given model with all fields, when to entity, then all fields mapped`() {
        val model = createFullSiteModel()

        val entity = mapper.toEntity(model)

        assertThat(entity.id).isEqualTo(model.id)
        assertThat(entity.siteId).isEqualTo(model.siteId)
        assertThat(entity.url).isEqualTo(model.url)
        assertThat(entity.adminUrl).isEqualTo(model.adminUrl)
        assertThat(entity.loginUrl).isEqualTo(model.loginUrl)
        assertThat(entity.name).isEqualTo(model.name)
        assertThat(entity.isWPCom).isEqualTo(model.isWPCom)
        assertThat(entity.isWPComAtomic).isEqualTo(model.isWPComAtomic)
        assertThat(entity.origin).isEqualTo(model.origin)
        assertThat(entity.selfHostedSiteId).isEqualTo(model.selfHostedSiteId)
        assertThat(entity.username).isEqualTo(model.username)
        assertThat(entity.password).isEqualTo(model.password)
        assertThat(entity.xmlRpcUrl).isEqualTo(model.xmlRpcUrl)
        assertThat(entity.wpApiRestUrl).isEqualTo(model.wpApiRestUrl)
        assertThat(entity.isJetpackInstalled).isEqualTo(model.isJetpackInstalled)
        assertThat(entity.isJetpackConnected).isEqualTo(model.isJetpackConnected)
        assertThat(entity.isJetpackCPConnected).isEqualTo(model.isJetpackCPConnected)
        assertThat(entity.hasWooCommerce).isEqualTo(model.hasWooCommerce)
        assertThat(entity.isPrivate).isEqualTo(model.isPrivate)
        assertThat(entity.planId).isEqualTo(model.planId)
        assertThat(entity.planShortName).isEqualTo(model.planShortName)
        assertThat(entity.hasCapabilityManageOptions).isEqualTo(model.hasCapabilityManageOptions)
        assertThat(entity.timezone).isEqualTo(model.timezone)
        assertThat(entity.applicationPasswordsAuthorizeUrl).isEqualTo(model.applicationPasswordsAuthorizeUrl)
        assertThat(entity.isGardenSite).isEqualTo(model.isGardenSite)
        assertThat(entity.gardenName).isEqualTo(model.gardenName)
        assertThat(entity.gardenPartner).isEqualTo(model.gardenPartner)
    }

    @Test
    fun `given model with null fields, when to entity, then mapped to empty strings`() {
        val model = SiteModel()

        val entity = mapper.toEntity(model)

        assertThat(entity.url).isEmpty()
        assertThat(entity.adminUrl).isEmpty()
        assertThat(entity.loginUrl).isEmpty()
        assertThat(entity.name).isEmpty()
        assertThat(entity.timezone).isEmpty()
        assertThat(entity.username).isEmpty()
        assertThat(entity.password).isEmpty()
        assertThat(entity.xmlRpcUrl).isEmpty()
        assertThat(entity.wpApiRestUrl).isEmpty()
        assertThat(entity.email).isEmpty()
        assertThat(entity.displayName).isEmpty()
        assertThat(entity.jetpackVersion).isEmpty()
        assertThat(entity.jetpackUserEmail).isEmpty()
        assertThat(entity.planShortName).isEmpty()
        assertThat(entity.planProductSlug).isEmpty()
        assertThat(entity.activeJetpackConnectionPlugins).isEmpty()
        assertThat(entity.jetpackModules).isEmpty()
        assertThat(entity.applicationPasswordsAuthorizeUrl).isEmpty()
        assertThat(entity.planActiveFeatures).isEmpty()
        assertThat(entity.gardenName).isEmpty()
        assertThat(entity.gardenPartner).isEmpty()
    }

    @Test
    fun `given model with defaults, when to entity, then defaults preserved`() {
        val model = SiteModel()

        val entity = mapper.toEntity(model)

        assertThat(entity.publishedStatus).isEqualTo(-1)
        assertThat(entity.origin).isEqualTo(SiteModel.ORIGIN_UNKNOWN)
    }

    // endregion

    // region toModel

    @Test
    fun `given entity with all fields, when to model, then all fields mapped`() {
        val entity = createFullSiteEntity()

        val model = mapper.toModel(entity)

        assertThat(model.id).isEqualTo(entity.id)
        assertThat(model.siteId).isEqualTo(entity.siteId)
        assertThat(model.url).isEqualTo(entity.url)
        assertThat(model.adminUrl).isEqualTo(entity.adminUrl)
        assertThat(model.loginUrl).isEqualTo(entity.loginUrl)
        assertThat(model.name).isEqualTo(entity.name)
        assertThat(model.isWPCom).isEqualTo(entity.isWPCom)
        assertThat(model.isWPComAtomic).isEqualTo(entity.isWPComAtomic)
        assertThat(model.origin).isEqualTo(entity.origin)
        assertThat(model.selfHostedSiteId).isEqualTo(entity.selfHostedSiteId)
        assertThat(model.username).isEqualTo(entity.username)
        assertThat(model.password).isEqualTo(entity.password)
        assertThat(model.xmlRpcUrl).isEqualTo(entity.xmlRpcUrl)
        assertThat(model.wpApiRestUrl).isEqualTo(entity.wpApiRestUrl)
        assertThat(model.isJetpackInstalled).isEqualTo(entity.isJetpackInstalled)
        assertThat(model.isJetpackConnected).isEqualTo(entity.isJetpackConnected)
        assertThat(model.isJetpackCPConnected).isEqualTo(entity.isJetpackCPConnected)
        assertThat(model.hasWooCommerce).isEqualTo(entity.hasWooCommerce)
        assertThat(model.isPrivate).isEqualTo(entity.isPrivate)
        assertThat(model.planId).isEqualTo(entity.planId)
        assertThat(model.planShortName).isEqualTo(entity.planShortName)
        assertThat(model.hasCapabilityManageOptions).isEqualTo(entity.hasCapabilityManageOptions)
        assertThat(model.timezone).isEqualTo(entity.timezone)
        assertThat(model.applicationPasswordsAuthorizeUrl).isEqualTo(entity.applicationPasswordsAuthorizeUrl)
        assertThat(model.isGardenSite).isEqualTo(entity.isGardenSite)
        assertThat(model.gardenName).isEqualTo(entity.gardenName)
        assertThat(model.gardenPartner).isEqualTo(entity.gardenPartner)
    }

    @Test
    fun `given default entity, when to model, then all strings are empty`() {
        val entity = mapper.toEntity(SiteModel())

        val model = mapper.toModel(entity)

        assertThat(model.url).isEmpty()
        assertThat(model.adminUrl).isEmpty()
        assertThat(model.loginUrl).isEmpty()
        assertThat(model.name).isEmpty()
        assertThat(model.timezone).isEmpty()
        assertThat(model.username).isEmpty()
        assertThat(model.password).isEmpty()
        assertThat(model.xmlRpcUrl).isEmpty()
        assertThat(model.wpApiRestUrl).isEmpty()
        assertThat(model.email).isEmpty()
        assertThat(model.displayName).isEmpty()
        assertThat(model.jetpackVersion).isEmpty()
        assertThat(model.jetpackUserEmail).isEmpty()
        assertThat(model.planShortName).isEmpty()
        assertThat(model.planProductSlug).isEmpty()
        assertThat(model.activeJetpackConnectionPlugins).isEmpty()
        assertThat(model.jetpackModules).isEmpty()
        assertThat(model.applicationPasswordsAuthorizeUrl).isEmpty()
        assertThat(model.planActiveFeatures).isEmpty()
        assertThat(model.gardenName).isEmpty()
        assertThat(model.gardenPartner).isEmpty()
    }

    // endregion

    // region Round-trip

    @Test
    fun `given model, when round trip through entity, then model matches`() {
        val original = createFullSiteModel()

        val roundTripped = mapper.toModel(mapper.toEntity(original))

        assertThat(roundTripped.id).isEqualTo(original.id)
        assertThat(roundTripped.siteId).isEqualTo(original.siteId)
        assertThat(roundTripped.name).isEqualTo(original.name)
        assertThat(roundTripped.hasWooCommerce).isEqualTo(original.hasWooCommerce)
        assertThat(roundTripped.isWPCom).isEqualTo(original.isWPCom)
        assertThat(roundTripped.origin).isEqualTo(original.origin)
        assertThat(roundTripped.isGardenSite).isEqualTo(original.isGardenSite)
        assertThat(roundTripped.gardenName).isEqualTo(original.gardenName)
    }

    // endregion

    /* HELPER */

    @Suppress("MagicNumber")
    private fun createFullSiteModel() = SiteModel().apply {
        id = 1
        siteId = 42L
        setUrl("https://test.wordpress.com")
        adminUrl = "https://test.wordpress.com/wp-admin"
        loginUrl = "https://test.wordpress.com/wp-login.php"
        name = "Test Site"
        setIsWPCom(true)
        setIsWPComAtomic(true)
        origin = SiteModel.ORIGIN_WPCOM_REST
        selfHostedSiteId = 0L
        username = "testuser"
        password = "testpass"
        xmlRpcUrl = "https://test.wordpress.com/xmlrpc.php"
        wpApiRestUrl = "https://test.wordpress.com/wp-json"
        setIsJetpackInstalled(false)
        setIsJetpackConnected(false)
        setIsJetpackCPConnected(false)
        setHasWooCommerce(true)
        planId = 100L
        planShortName = "Business"
        setHasCapabilityManageOptions(true)
        timezone = "-5"
        applicationPasswordsAuthorizeUrl = "https://test.wordpress.com/authorize"
        setIsGardenSite(true)
        gardenName = "commerce"
        gardenPartner = "woo"
    }

    @Suppress("MagicNumber")
    private fun createFullSiteEntity() = SiteEntity(
        id = 1,
        siteId = 42L,
        url = "https://test.wordpress.com",
        adminUrl = "https://test.wordpress.com/wp-admin",
        loginUrl = "https://test.wordpress.com/wp-login.php",
        name = "Test Site",
        isWPCom = true,
        isWPComAtomic = true,
        publishedStatus = 0,
        timezone = "-5",
        origin = SiteModel.ORIGIN_WPCOM_REST,
        selfHostedSiteId = 0L,
        username = "testuser",
        password = "testpass",
        xmlRpcUrl = "https://test.wordpress.com/xmlrpc.php",
        wpApiRestUrl = "https://test.wordpress.com/wp-json",
        email = "",
        displayName = "",
        isJetpackInstalled = false,
        isJetpackConnected = false,
        isJetpackCPConnected = false,
        jetpackVersion = "",
        jetpackUserEmail = "",
        isWpComStore = false,
        hasWooCommerce = true,
        isPrivate = false,
        planId = 100L,
        planShortName = "Business",
        planProductSlug = "",
        hasCapabilityManageOptions = true,
        activeJetpackConnectionPlugins = "",
        jetpackModules = "",
        applicationPasswordsAuthorizeUrl = "https://test.wordpress.com/authorize",
        canBlaze = false,
        planActiveFeatures = "",
        isGardenSite = true,
        gardenName = "commerce",
        gardenPartner = "woo"
    )
}
