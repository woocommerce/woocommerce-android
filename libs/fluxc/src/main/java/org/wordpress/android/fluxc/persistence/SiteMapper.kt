package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.SiteEntity
import javax.inject.Inject

class SiteMapper @Inject constructor() {
    fun toEntity(model: SiteModel) = SiteEntity(
        id = model.id,
        siteId = model.siteId,
        url = model.url.orEmpty(),
        adminUrl = model.adminUrl.orEmpty(),
        loginUrl = model.loginUrl.orEmpty(),
        name = model.name.orEmpty(),
        isWPCom = model.isWPCom,
        isWPComAtomic = model.isWPComAtomic,
        publishedStatus = model.publishedStatus,
        timezone = model.timezone.orEmpty(),
        origin = model.origin,
        selfHostedSiteId = model.selfHostedSiteId,
        username = model.username.orEmpty(),
        password = model.password.orEmpty(),
        xmlRpcUrl = model.xmlRpcUrl.orEmpty(),
        wpApiRestUrl = model.wpApiRestUrl.orEmpty(),
        email = model.email.orEmpty(),
        displayName = model.displayName.orEmpty(),
        isJetpackInstalled = model.isJetpackInstalled,
        isJetpackConnected = model.isJetpackConnected,
        isJetpackCPConnected = model.isJetpackCPConnected,
        jetpackVersion = model.jetpackVersion.orEmpty(),
        jetpackUserEmail = model.jetpackUserEmail.orEmpty(),
        isWpComStore = model.isWpComStore,
        hasWooCommerce = model.hasWooCommerce,
        isPrivate = model.isPrivate,
        planId = model.planId,
        planShortName = model.planShortName.orEmpty(),
        planProductSlug = model.planProductSlug.orEmpty(),
        hasCapabilityManageOptions = model.hasCapabilityManageOptions,
        activeJetpackConnectionPlugins = model.activeJetpackConnectionPlugins.orEmpty(),
        jetpackModules = model.jetpackModules.orEmpty(),
        applicationPasswordsAuthorizeUrl = model.applicationPasswordsAuthorizeUrl.orEmpty(),
        canBlaze = model.canBlaze,
        planActiveFeatures = model.planActiveFeatures.orEmpty(),
        isGardenSite = model.isGardenSite,
        gardenName = model.gardenName.orEmpty(),
        gardenPartner = model.gardenPartner.orEmpty(),
    )

    fun toModel(entity: SiteEntity) = SiteModel().apply {
        id = entity.id
        siteId = entity.siteId
        url = entity.url
        adminUrl = entity.adminUrl
        loginUrl = entity.loginUrl
        name = entity.name
        setIsWPCom(entity.isWPCom)
        setIsWPComAtomic(entity.isWPComAtomic)
        publishedStatus = entity.publishedStatus
        timezone = entity.timezone
        origin = entity.origin
        selfHostedSiteId = entity.selfHostedSiteId
        username = entity.username
        password = entity.password
        xmlRpcUrl = entity.xmlRpcUrl
        wpApiRestUrl = entity.wpApiRestUrl
        email = entity.email
        displayName = entity.displayName
        setIsJetpackInstalled(entity.isJetpackInstalled)
        setIsJetpackConnected(entity.isJetpackConnected)
        setIsJetpackCPConnected(entity.isJetpackCPConnected)
        jetpackVersion = entity.jetpackVersion
        jetpackUserEmail = entity.jetpackUserEmail
        setIsWpComStore(entity.isWpComStore)
        setHasWooCommerce(entity.hasWooCommerce)
        setIsPrivate(entity.isPrivate)
        planId = entity.planId
        planShortName = entity.planShortName
        planProductSlug = entity.planProductSlug
        setHasCapabilityManageOptions(entity.hasCapabilityManageOptions)
        activeJetpackConnectionPlugins = entity.activeJetpackConnectionPlugins
        jetpackModules = entity.jetpackModules
        applicationPasswordsAuthorizeUrl = entity.applicationPasswordsAuthorizeUrl
        setCanBlaze(entity.canBlaze)
        planActiveFeatures = entity.planActiveFeatures
        setIsGardenSite(entity.isGardenSite)
        gardenName = entity.gardenName
        gardenPartner = entity.gardenPartner
    }
}
