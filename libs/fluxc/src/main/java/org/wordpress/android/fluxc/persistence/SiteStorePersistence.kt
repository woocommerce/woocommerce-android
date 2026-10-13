package org.wordpress.android.fluxc.persistence

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import com.wellsql.generated.SiteModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiteStorePersistence @Inject constructor(
    private val accountStorePersistence: AccountStorePersistence,
) {
    class DuplicateSiteException : Exception()

    data class LegacySitePassword(val url: String, val username: String?, val password: String)

    /**
     * Inserts the given SiteModel into the DB, or updates an existing entry where sites match.
     *
     * Possible cases:
     * 1. Exists in the DB already and matches by local id (simple update) -> UPDATE
     * 2. Exists in the DB, is a Jetpack or WordPress site and matches by remote id (SITE_ID) -> UPDATE
     * 3. Exists in the DB, is a pure self hosted and matches by remote id (SITE_ID) + URL -> UPDATE
     * 4. Not matching any previous cases -> INSERT
     *
     * **Return value:** `1` on success (both insert and update), `0` if
     * the WP.com account validation fails. The return value does not distinguish between
     * insert and update — callers like [SiteStore.createOrUpdateSites] use it only as a
     * success/failure indicator.
     *
     * **Side effect:** mutates [site]`.id` to the local ID assigned by the DB (on insert) or
     * to the existing row's local ID (on update). This is required because callers such as
     * [SiteStore.createOrUpdateSites] add the same [site] object to the `updatedSites` list
     * after this call, and downstream consumers expect `site.id` to carry the correct local ID.
     */
    @Suppress("LongMethod", "ComplexMethod", "ReturnCount")
    @Throws(DuplicateSiteException::class)
    fun insertOrUpdateSite(site: SiteModel): Int {
        // If we're inserting or updating a WP.com REST API site, validate that we actually have a WordPress.com
        // account present
        // This prevents a late UPDATE_SITES action from re-populating the database after sign out from WordPress.com
        if (site.isUsingWpComRestApi) {
            val account = accountStorePersistence.getDefaultAccount()
            if (account == null || account.userId == 0L) {
                AppLog.w(AppLog.T.DB, "Can't insert WP.com site " + site.url + ", missing user account")
                return 0
            }
        }

        // If the site already exist and has an id, we want to update it.
        var siteResult = WellSql.select(SiteModel::class.java)
                .where().beginGroup()
                .equals(SiteModelTable.ID, site.id)
                .endGroup().endWhere().asModel
        if (!siteResult.isEmpty()) {
            AppLog.d(AppLog.T.DB, "Site found by (local) ID: " + site.id)
        }

        // Looks like a new site, make sure we don't already have it.
        if (siteResult.isEmpty()) {
            if (site.siteId > 0) {
                // For WordPress.com and Jetpack sites, the WP.com ID is a unique enough identifier
                siteResult = WellSql.select(SiteModel::class.java)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, site.siteId)
                        .endGroup().endWhere().asModel
                if (!siteResult.isEmpty()) {
                    AppLog.d(AppLog.T.DB, "Site found by SITE_ID: " + site.siteId)
                }
            } else {
                siteResult = WellSql.select(SiteModel::class.java)
                        .where().beginGroup()
                        .equals(SiteModelTable.SITE_ID, site.siteId)
                        .equals(SiteModelTable.URL, site.url)
                        .endGroup().endWhere().asModel
                if (!siteResult.isEmpty()) {
                    AppLog.d(AppLog.T.DB, "Site found by SITE_ID: " + site.siteId + " and URL: " + site.url)
                }
            }
        }

        return if (siteResult.isEmpty()) {
            // No site with this local ID, REMOTE_ID, or REMOTE_ID + URL, then insert it
            AppLog.d(AppLog.T.DB, "Inserting site: " + site.url)
            WellSql.insert(site).asSingleTransaction(true).execute()
            1
        } else {
            // Update old site
            AppLog.d(AppLog.T.DB, "Updating site: " + site.url)
            val oldId = siteResult[0].id
            try {
                WellSql.update(SiteModel::class.java).whereId(oldId)
                        .put(site, UpdateAllExceptId(SiteModel::class.java)).execute()
            } catch (e: SQLiteConstraintException) {
                AppLog.e(
                    AppLog.T.DB,
                    "Error while updating site: siteId=${site.siteId} url=${site.url}",
                    e
                )
                throw DuplicateSiteException()
            }
        }
    }

    /**
     * Site passwords used to be kept in plain text in the `PASSWORD` column, which is no longer mapped to
     * [SiteModel]. Databases created since then don't have the column at all.
     */
    fun getLegacySitePasswords(): List<LegacySitePassword> {
        val db = WellSql.giveMeReadableDb()
        if (!db.hasLegacyPasswordColumn()) return emptyList()

        return db.rawQuery(
            "SELECT URL, USERNAME, PASSWORD FROM SiteModel WHERE PASSWORD IS NOT NULL AND PASSWORD != ''",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val url = cursor.getString(0) ?: continue
                    add(LegacySitePassword(url = url, username = cursor.getString(1), password = cursor.getString(2)))
                }
            }
        }
    }

    fun clearLegacySitePasswords() {
        val db = WellSql.giveMeWritableDb()
        if (db.hasLegacyPasswordColumn()) {
            db.execSQL("UPDATE SiteModel SET PASSWORD = NULL")
        }
    }

    private fun SQLiteDatabase.hasLegacyPasswordColumn(): Boolean =
        rawQuery("PRAGMA table_info(SiteModel)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            generateSequence { cursor.takeIf { it.moveToNext() } }.any { it.getString(nameIndex) == "PASSWORD" }
        }
}
