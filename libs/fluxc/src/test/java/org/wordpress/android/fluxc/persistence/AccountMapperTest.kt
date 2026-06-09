package org.wordpress.android.fluxc.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.persistence.dao.AccountDao
import org.wordpress.android.fluxc.persistence.entity.AccountEntity

class AccountMapperTest {
    private val mapper = AccountMapper()

    @Test
    fun `given account model, when to entity, then all fields are mapped`() {
        val model = createTestAccountModel()

        val entity = mapper.toEntity(model)

        assertThat(entity.id).isEqualTo(model.id)
        assertThat(entity.userName).isEqualTo(model.userName)
        assertThat(entity.userId).isEqualTo(model.userId)
        assertThat(entity.displayName).isEqualTo(model.displayName)
        assertThat(entity.profileUrl).isEqualTo(model.profileUrl)
        assertThat(entity.avatarUrl).isEqualTo(model.avatarUrl)
        assertThat(entity.primarySiteId).isEqualTo(model.primarySiteId)
        assertThat(entity.emailVerified).isEqualTo(model.emailVerified)
        assertThat(entity.siteCount).isEqualTo(model.siteCount)
        assertThat(entity.visibleSiteCount).isEqualTo(model.visibleSiteCount)
        assertThat(entity.email).isEqualTo(model.email)
        assertThat(entity.hasUnseenNotes).isEqualTo(model.hasUnseenNotes)
        assertThat(entity.firstName).isEqualTo(model.firstName)
        assertThat(entity.lastName).isEqualTo(model.lastName)
        assertThat(entity.aboutMe).isEqualTo(model.aboutMe)
        assertThat(entity.date).isEqualTo(model.date)
        assertThat(entity.newEmail).isEqualTo(model.newEmail)
        assertThat(entity.pendingEmailChange).isEqualTo(model.pendingEmailChange)
        assertThat(entity.twoStepEnabled).isEqualTo(model.twoStepEnabled)
        assertThat(entity.webAddress).isEqualTo(model.webAddress)
        assertThat(entity.tracksOptOut).isEqualTo(model.tracksOptOut)
        assertThat(entity.usernameCanBeChanged).isEqualTo(model.usernameCanBeChanged)
    }

    @Test
    fun `given account entity, when to model, then all fields are mapped`() {
        val entity = createTestAccountEntity()

        val model = mapper.toModel(entity)

        assertThat(model.id).isEqualTo(entity.id)
        assertThat(model.userName).isEqualTo(entity.userName)
        assertThat(model.userId).isEqualTo(entity.userId)
        assertThat(model.displayName).isEqualTo(entity.displayName)
        assertThat(model.profileUrl).isEqualTo(entity.profileUrl)
        assertThat(model.avatarUrl).isEqualTo(entity.avatarUrl)
        assertThat(model.primarySiteId).isEqualTo(entity.primarySiteId)
        assertThat(model.emailVerified).isEqualTo(entity.emailVerified)
        assertThat(model.siteCount).isEqualTo(entity.siteCount)
        assertThat(model.visibleSiteCount).isEqualTo(entity.visibleSiteCount)
        assertThat(model.email).isEqualTo(entity.email)
        assertThat(model.hasUnseenNotes).isEqualTo(entity.hasUnseenNotes)
        assertThat(model.firstName).isEqualTo(entity.firstName)
        assertThat(model.lastName).isEqualTo(entity.lastName)
        assertThat(model.aboutMe).isEqualTo(entity.aboutMe)
        assertThat(model.date).isEqualTo(entity.date)
        assertThat(model.newEmail).isEqualTo(entity.newEmail)
        assertThat(model.pendingEmailChange).isEqualTo(entity.pendingEmailChange)
        assertThat(model.twoStepEnabled).isEqualTo(entity.twoStepEnabled)
        assertThat(model.webAddress).isEqualTo(entity.webAddress)
        assertThat(model.tracksOptOut).isEqualTo(entity.tracksOptOut)
        assertThat(model.usernameCanBeChanged).isEqualTo(entity.usernameCanBeChanged)
    }

    @Test
    fun `given account model with null strings, when to entity, then strings are empty`() {
        val model = AccountModel().apply {
            id = AccountDao.DEFAULT_ACCOUNT_LOCAL_ID
            userId = 1L
        }

        val entity = mapper.toEntity(model)

        assertThat(entity.userName).isEmpty()
        assertThat(entity.displayName).isEmpty()
        assertThat(entity.profileUrl).isEmpty()
        assertThat(entity.avatarUrl).isEmpty()
        assertThat(entity.email).isEmpty()
        assertThat(entity.firstName).isEmpty()
        assertThat(entity.lastName).isEmpty()
        assertThat(entity.aboutMe).isEmpty()
        assertThat(entity.date).isEmpty()
        assertThat(entity.newEmail).isEmpty()
        assertThat(entity.webAddress).isEmpty()
    }

    @Test
    fun `given account model, when round trip, then data is preserved`() {
        val original = createTestAccountModel()

        val entity = mapper.toEntity(original)
        val restored = mapper.toModel(entity)

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `given account model with any id, when to entity, then id is default`() {
        val model = AccountModel().apply {
            id = 999
            userId = 1L
        }

        val entity = mapper.toEntity(model)

        assertThat(entity.id).isEqualTo(AccountDao.DEFAULT_ACCOUNT_LOCAL_ID)
    }

    /* HELPER */

    private fun createTestAccountModel() = AccountModel().apply {
        id = AccountDao.DEFAULT_ACCOUNT_LOCAL_ID
        userName = "testUser"
        userId = 123L
        displayName = "Test User"
        profileUrl = "https://profile.url"
        avatarUrl = "https://avatar.url"
        primarySiteId = 456L
        emailVerified = true
        siteCount = 5
        visibleSiteCount = 3
        email = "test@example.com"
        hasUnseenNotes = false
        firstName = "Test"
        lastName = "User"
        aboutMe = "About me"
        date = "2024-01-01"
        newEmail = "new@example.com"
        pendingEmailChange = true
        twoStepEnabled = true
        webAddress = "https://web.address"
        tracksOptOut = false
        usernameCanBeChanged = true
    }

    private fun createTestAccountEntity() = AccountEntity(
        id = AccountDao.DEFAULT_ACCOUNT_LOCAL_ID,
        userName = "testUser",
        userId = 123L,
        displayName = "Test User",
        profileUrl = "https://profile.url",
        avatarUrl = "https://avatar.url",
        primarySiteId = 456L,
        emailVerified = true,
        siteCount = 5,
        visibleSiteCount = 3,
        email = "test@example.com",
        hasUnseenNotes = false,
        firstName = "Test",
        lastName = "User",
        aboutMe = "About me",
        date = "2024-01-01",
        newEmail = "new@example.com",
        pendingEmailChange = true,
        twoStepEnabled = true,
        webAddress = "https://web.address",
        tracksOptOut = false,
        usernameCanBeChanged = true,
    )
}
