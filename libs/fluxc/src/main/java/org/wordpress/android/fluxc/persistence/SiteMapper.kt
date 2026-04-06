package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.entity.SiteEntity
import javax.inject.Inject

class SiteMapper @Inject constructor() {
    @Suppress("LongMethod")
    fun toEntity(model: SiteModel) = SiteEntity(
        id = model.id,
        siteId = model.siteId,
        url = model.url.orEmpty(),
        adminUrl = model.adminUrl.orEmpty(),
        loginUrl = model.loginUrl.orEmpty(),
        name = model.name.orEmpty(),
        description = model.description.orEmpty(),
        isWPCom = model.isWPCom,
        isWPComAtomic = model.isWPComAtomic,
        publishedStatus = model.publishedStatus,
        isFeaturedImageSupported = model.isFeaturedImageSupported,
        isWpForTeamsSite = model.isWpForTeamsSite,
        defaultCommentStatus = model.defaultCommentStatus.orEmpty(),
        timezone = model.timezone.orEmpty(),
        frameNonce = model.frameNonce.orEmpty(),
        maxUploadSize = model.maxUploadSize,
        memoryLimit = model.memoryLimit,
        origin = model.origin,
        organizationId = model.organizationId,
        showOnFront = model.showOnFront.orEmpty(),
        pageOnFront = model.pageOnFront,
        pageForPosts = model.pageForPosts,
        selfHostedSiteId = model.selfHostedSiteId,
        username = model.username.orEmpty(),
        password = model.password.orEmpty(),
        xmlRpcUrl = model.xmlRpcUrl.orEmpty(),
        wpApiRestUrl = model.wpApiRestUrl.orEmpty(),
        softwareVersion = model.softwareVersion.orEmpty(),
        isSelfHostedAdmin = model.isSelfHostedAdmin,
        email = model.email.orEmpty(),
        displayName = model.displayName.orEmpty(),
        isJetpackInstalled = model.isJetpackInstalled,
        isJetpackConnected = model.isJetpackConnected,
        isJetpackCPConnected = model.isJetpackCPConnected,
        jetpackVersion = model.jetpackVersion.orEmpty(),
        jetpackUserEmail = model.jetpackUserEmail.orEmpty(),
        isAutomatedTransfer = model.isAutomatedTransfer,
        isWpComStore = model.isWpComStore,
        hasWooCommerce = model.hasWooCommerce,
        isVisible = model.isVisible,
        isPrivate = model.isPrivate,
        isComingSoon = model.isComingSoon,
        isVideoPressSupported = model.isVideoPressSupported,
        planId = model.planId,
        planShortName = model.planShortName.orEmpty(),
        planProductSlug = model.planProductSlug.orEmpty(),
        iconUrl = model.iconUrl.orEmpty(),
        hasFreePlan = model.hasFreePlan,
        unmappedUrl = model.unmappedUrl.orEmpty(),
        webEditor = model.webEditor.orEmpty(),
        mobileEditor = model.mobileEditor.orEmpty(),
        hasCapabilityEditPages = model.hasCapabilityEditPages,
        hasCapabilityEditPosts = model.hasCapabilityEditPosts,
        hasCapabilityEditOthersPosts = model.hasCapabilityEditOthersPosts,
        hasCapabilityEditOthersPages = model.hasCapabilityEditOthersPages,
        hasCapabilityDeletePosts = model.hasCapabilityDeletePosts,
        hasCapabilityDeleteOthersPosts = model.hasCapabilityDeleteOthersPosts,
        hasCapabilityEditThemeOptions = model.hasCapabilityEditThemeOptions,
        hasCapabilityEditUsers = model.hasCapabilityEditUsers,
        hasCapabilityListUsers = model.hasCapabilityListUsers,
        hasCapabilityManageCategories = model.hasCapabilityManageCategories,
        hasCapabilityManageOptions = model.hasCapabilityManageOptions,
        hasCapabilityActivateWordads = model.hasCapabilityActivateWordads,
        hasCapabilityPromoteUsers = model.hasCapabilityPromoteUsers,
        hasCapabilityPublishPosts = model.hasCapabilityPublishPosts,
        hasCapabilityUploadFiles = model.hasCapabilityUploadFiles,
        hasCapabilityDeleteUser = model.hasCapabilityDeleteUser,
        hasCapabilityRemoveUsers = model.hasCapabilityRemoveUsers,
        hasCapabilityViewStats = model.hasCapabilityViewStats,
        spaceAvailable = model.spaceAvailable,
        spaceAllowed = model.spaceAllowed,
        spaceUsed = model.spaceUsed,
        spacePercentUsed = model.spacePercentUsed,
        activeModules = model.activeModules.orEmpty(),
        isPublicizePermanentlyDisabled = model.isPublicizePermanentlyDisabled,
        activeJetpackConnectionPlugins = model.activeJetpackConnectionPlugins.orEmpty(),
        jetpackModules = model.jetpackModules.orEmpty(),
        zendeskPlan = model.zendeskPlan.orEmpty(),
        zendeskAddOns = model.zendeskAddOns.orEmpty(),
        isBloggingPromptsOptedIn = model.isBloggingPromptsOptedIn,
        isBloggingPromptsCardOptedIn = model.isBloggingPromptsCardOptedIn,
        isPotentialBloggingSite = model.isPotentialBloggingSite,
        isBloggingReminderOnMonday = model.isBloggingReminderOnMonday,
        isBloggingReminderOnTuesday = model.isBloggingReminderOnTuesday,
        isBloggingReminderOnWednesday = model.isBloggingReminderOnWednesday,
        isBloggingReminderOnThursday = model.isBloggingReminderOnThursday,
        isBloggingReminderOnFriday = model.isBloggingReminderOnFriday,
        isBloggingReminderOnSaturday = model.isBloggingReminderOnSaturday,
        isBloggingReminderOnSunday = model.isBloggingReminderOnSunday,
        bloggingReminderHour = model.bloggingReminderHour,
        bloggingReminderMinute = model.bloggingReminderMinute,
        applicationPasswordsAuthorizeUrl = model.applicationPasswordsAuthorizeUrl.orEmpty(),
        canBlaze = model.canBlaze,
        planActiveFeatures = model.planActiveFeatures.orEmpty(),
        wasEcommerceTrial = model.wasEcommerceTrial,
        isSingleUserSite = model.isSingleUserSite(),
        isGardenSite = model.isGardenSite,
        gardenName = model.gardenName.orEmpty(),
        gardenPartner = model.gardenPartner.orEmpty(),
    )

    @Suppress("LongMethod")
    fun toModel(entity: SiteEntity) = SiteModel().apply {
        id = entity.id
        siteId = entity.siteId
        url = entity.url
        adminUrl = entity.adminUrl
        loginUrl = entity.loginUrl
        name = entity.name
        description = entity.description
        setIsWPCom(entity.isWPCom)
        setIsWPComAtomic(entity.isWPComAtomic)
        publishedStatus = entity.publishedStatus
        setIsFeaturedImageSupported(entity.isFeaturedImageSupported)
        setIsWpForTeamsSite(entity.isWpForTeamsSite)
        defaultCommentStatus = entity.defaultCommentStatus
        timezone = entity.timezone
        frameNonce = entity.frameNonce
        maxUploadSize = entity.maxUploadSize
        memoryLimit = entity.memoryLimit
        origin = entity.origin
        organizationId = entity.organizationId
        showOnFront = entity.showOnFront
        pageOnFront = entity.pageOnFront
        pageForPosts = entity.pageForPosts
        selfHostedSiteId = entity.selfHostedSiteId
        username = entity.username
        password = entity.password
        xmlRpcUrl = entity.xmlRpcUrl
        wpApiRestUrl = entity.wpApiRestUrl
        softwareVersion = entity.softwareVersion
        setIsSelfHostedAdmin(entity.isSelfHostedAdmin)
        email = entity.email
        displayName = entity.displayName
        setIsJetpackInstalled(entity.isJetpackInstalled)
        setIsJetpackConnected(entity.isJetpackConnected)
        setIsJetpackCPConnected(entity.isJetpackCPConnected)
        jetpackVersion = entity.jetpackVersion
        jetpackUserEmail = entity.jetpackUserEmail
        setIsAutomatedTransfer(entity.isAutomatedTransfer)
        setIsWpComStore(entity.isWpComStore)
        setHasWooCommerce(entity.hasWooCommerce)
        setIsVisible(entity.isVisible)
        setIsPrivate(entity.isPrivate)
        setIsComingSoon(entity.isComingSoon)
        setIsVideoPressSupported(entity.isVideoPressSupported)
        planId = entity.planId
        planShortName = entity.planShortName
        planProductSlug = entity.planProductSlug
        iconUrl = entity.iconUrl
        setHasFreePlan(entity.hasFreePlan)
        unmappedUrl = entity.unmappedUrl
        webEditor = entity.webEditor
        mobileEditor = entity.mobileEditor
        setHasCapabilityEditPages(entity.hasCapabilityEditPages)
        setHasCapabilityEditPosts(entity.hasCapabilityEditPosts)
        setHasCapabilityEditOthersPosts(entity.hasCapabilityEditOthersPosts)
        setHasCapabilityEditOthersPages(entity.hasCapabilityEditOthersPages)
        setHasCapabilityDeletePosts(entity.hasCapabilityDeletePosts)
        setHasCapabilityDeleteOthersPosts(entity.hasCapabilityDeleteOthersPosts)
        setHasCapabilityEditThemeOptions(entity.hasCapabilityEditThemeOptions)
        setHasCapabilityEditUsers(entity.hasCapabilityEditUsers)
        setHasCapabilityListUsers(entity.hasCapabilityListUsers)
        setHasCapabilityManageCategories(entity.hasCapabilityManageCategories)
        setHasCapabilityManageOptions(entity.hasCapabilityManageOptions)
        setHasCapabilityActivateWordads(entity.hasCapabilityActivateWordads)
        setHasCapabilityPromoteUsers(entity.hasCapabilityPromoteUsers)
        setHasCapabilityPublishPosts(entity.hasCapabilityPublishPosts)
        setHasCapabilityUploadFiles(entity.hasCapabilityUploadFiles)
        setHasCapabilityDeleteUser(entity.hasCapabilityDeleteUser)
        setHasCapabilityRemoveUsers(entity.hasCapabilityRemoveUsers)
        setHasCapabilityViewStats(entity.hasCapabilityViewStats)
        spaceAvailable = entity.spaceAvailable
        spaceAllowed = entity.spaceAllowed
        spaceUsed = entity.spaceUsed
        spacePercentUsed = entity.spacePercentUsed
        activeModules = entity.activeModules
        setIsPublicizePermanentlyDisabled(entity.isPublicizePermanentlyDisabled)
        activeJetpackConnectionPlugins = entity.activeJetpackConnectionPlugins
        jetpackModules = entity.jetpackModules
        zendeskPlan = entity.zendeskPlan
        zendeskAddOns = entity.zendeskAddOns
        setIsBloggingPromptsOptedIn(entity.isBloggingPromptsOptedIn)
        setIsBloggingPromptsCardOptedIn(entity.isBloggingPromptsCardOptedIn)
        setIsPotentialBloggingSite(entity.isPotentialBloggingSite)
        setIsBloggingReminderOnMonday(entity.isBloggingReminderOnMonday)
        setIsBloggingReminderOnTuesday(entity.isBloggingReminderOnTuesday)
        setIsBloggingReminderOnWednesday(entity.isBloggingReminderOnWednesday)
        setIsBloggingReminderOnThursday(entity.isBloggingReminderOnThursday)
        setIsBloggingReminderOnFriday(entity.isBloggingReminderOnFriday)
        setIsBloggingReminderOnSaturday(entity.isBloggingReminderOnSaturday)
        setIsBloggingReminderOnSunday(entity.isBloggingReminderOnSunday)
        bloggingReminderHour = entity.bloggingReminderHour
        bloggingReminderMinute = entity.bloggingReminderMinute
        applicationPasswordsAuthorizeUrl = entity.applicationPasswordsAuthorizeUrl
        setCanBlaze(entity.canBlaze)
        planActiveFeatures = entity.planActiveFeatures
        wasEcommerceTrial = entity.wasEcommerceTrial
        setIsSingleUserSite(entity.isSingleUserSite)
        setIsGardenSite(entity.isGardenSite)
        gardenName = entity.gardenName
        gardenPartner = entity.gardenPartner
    }
}
