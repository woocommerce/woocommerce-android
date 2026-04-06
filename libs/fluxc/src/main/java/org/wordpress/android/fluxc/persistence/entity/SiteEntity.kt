package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a WordPress site (WP.com, Jetpack, or self-hosted).
 *
 * **Why [id] (auto-generated) instead of [siteId] as the primary key:**
 * [siteId] is the remote WordPress.com site ID, which is `0` for pure self-hosted
 * sites that have no WP.com connection — meaning multiple self-hosted sites would
 * share the same `siteId = 0`. The auto-generated [id] serves as the local surrogate
 * key (`localSiteId`) and is referenced throughout the codebase for site lookups,
 * selected-site persistence, and as the foreign key target for WooCommerce entity
 * tables (e.g., `ProductTagEntity`, `OrderSummaryEntity`).
 */
@Entity(
    tableName = "SiteEntity",
    indices = [Index(
        value = ["siteId", "url"],
        unique = true
    )]
)
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val siteId: Long,
    val url: String,
    val adminUrl: String,
    val loginUrl: String,
    val name: String,
    val isWPCom: Boolean,
    val isWPComAtomic: Boolean,
    val publishedStatus: Int,
    val timezone: String,
    val origin: Int,
    val selfHostedSiteId: Long,
    val username: String,
    val password: String,
    val xmlRpcUrl: String,
    val wpApiRestUrl: String,
    val email: String,
    val displayName: String,
    val isJetpackInstalled: Boolean,
    val isJetpackConnected: Boolean,
    val isJetpackCPConnected: Boolean,
    val jetpackVersion: String,
    val jetpackUserEmail: String,
    val isWpComStore: Boolean,
    val hasWooCommerce: Boolean,
    val isPrivate: Boolean,
    val planId: Long,
    val planShortName: String,
    val planProductSlug: String,
    val hasCapabilityManageOptions: Boolean,
    val activeJetpackConnectionPlugins: String,
    val jetpackModules: String,
    val applicationPasswordsAuthorizeUrl: String,
    val canBlaze: Boolean,
    val planActiveFeatures: String,
    val isGardenSite: Boolean,
    val gardenName: String,
    val gardenPartner: String,
)
