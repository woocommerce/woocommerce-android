package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.persistence.dao.AccountDao
import org.wordpress.android.fluxc.persistence.entity.AccountEntity
import javax.inject.Inject

class AccountMapper @Inject constructor() {
    fun toEntity(model: AccountModel) = AccountEntity(
        id = AccountDao.DEFAULT_ACCOUNT_LOCAL_ID,
        userName = model.userName.orEmpty(),
        userId = model.userId,
        displayName = model.displayName.orEmpty(),
        profileUrl = model.profileUrl.orEmpty(),
        avatarUrl = model.avatarUrl.orEmpty(),
        primarySiteId = model.primarySiteId,
        emailVerified = model.emailVerified,
        siteCount = model.siteCount,
        visibleSiteCount = model.visibleSiteCount,
        email = model.email.orEmpty(),
        hasUnseenNotes = model.hasUnseenNotes,
        firstName = model.firstName.orEmpty(),
        lastName = model.lastName.orEmpty(),
        aboutMe = model.aboutMe.orEmpty(),
        date = model.date.orEmpty(),
        newEmail = model.newEmail.orEmpty(),
        pendingEmailChange = model.pendingEmailChange,
        twoStepEnabled = model.twoStepEnabled,
        webAddress = model.webAddress.orEmpty(),
        tracksOptOut = model.tracksOptOut,
        usernameCanBeChanged = model.usernameCanBeChanged,
    )

    fun toModel(entity: AccountEntity) = AccountModel().apply {
        id = entity.id
        userName = entity.userName
        userId = entity.userId
        displayName = entity.displayName
        profileUrl = entity.profileUrl
        avatarUrl = entity.avatarUrl
        primarySiteId = entity.primarySiteId
        emailVerified = entity.emailVerified
        siteCount = entity.siteCount
        visibleSiteCount = entity.visibleSiteCount
        email = entity.email
        hasUnseenNotes = entity.hasUnseenNotes
        firstName = entity.firstName
        lastName = entity.lastName
        aboutMe = entity.aboutMe
        date = entity.date
        newEmail = entity.newEmail
        pendingEmailChange = entity.pendingEmailChange
        twoStepEnabled = entity.twoStepEnabled
        webAddress = entity.webAddress
        tracksOptOut = entity.tracksOptOut
        usernameCanBeChanged = entity.usernameCanBeChanged
    }
}
