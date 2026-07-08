package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.dao.AccountDao

/**
 * Room entity representing the logged-in WordPress.com account.
 *
 * This table holds exactly **one row** at any time. The app enforces a single-account design:
 * [AccountStorePersistence.insertOrUpdateDefaultAccount] always sets [id] to
 * [AccountDao.DEFAULT_ACCOUNT_LOCAL_ID] (= 1), and the only retrieval path is
 * [AccountDao.getDefaultAccount] which queries by that same fixed ID.
 * Login replaces the row; logout deletes it.
 *
 * **Why [id] instead of [userId] as the primary key:**
 * [userId] (the remote WordPress.com user ID) is `0` when the [AccountModel] is first
 * created — it is only populated after the `/me` network response arrives. Because the
 * account row can be inserted before that response, a fixed local surrogate key ([id] = 1)
 * avoids using `0` as a sentinel primary key.
 */
@Entity(tableName = "AccountEntity")
data class AccountEntity(
    @PrimaryKey(autoGenerate = false) val id: Int,
    val userName: String,
    val userId: Long,
    val displayName: String,
    val profileUrl: String,
    val avatarUrl: String,
    val primarySiteId: Long,
    val emailVerified: Boolean,
    val siteCount: Int,
    val visibleSiteCount: Int,
    val email: String,
    val hasUnseenNotes: Boolean,
    val firstName: String,
    val lastName: String,
    val aboutMe: String,
    val date: String,
    val newEmail: String,
    val pendingEmailChange: Boolean,
    val twoStepEnabled: Boolean,
    val webAddress: String,
    val tracksOptOut: Boolean,
    val crashReportingOptOut: Boolean?,
    val usernameCanBeChanged: Boolean,
)
