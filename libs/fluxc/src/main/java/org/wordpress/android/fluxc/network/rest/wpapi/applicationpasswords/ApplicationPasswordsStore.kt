package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.SiteStorePersistence
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsStore @Inject constructor(
    private val context: Context,
    private val siteStorePersistence: SiteStorePersistence
) {
    companion object {
        private const val USERNAME_PREFERENCE_KEY_PREFIX = "username_"
        private const val PASSWORD_PREFERENCE_KEY_PREFIX = "app_password_"
        private const val UUID_PREFERENCE_KEY_PREFIX = "app_password_uuid_"
        private const val SITE_USERNAME_PREFERENCE_KEY_PREFIX = "site_username_"
        private const val SITE_PASSWORD_PREFERENCE_KEY_PREFIX = "site_password_"
        const val APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR = "application_passwords_keystore_encryption_error"
    }

    @Inject internal lateinit var configuration: ApplicationPasswordsConfiguration

    private val applicationName: String
        get() = configuration.applicationName

    private val encryptedPreferences by lazy {
        initEncryptedPrefs()
    }

    private var areLegacySitePasswordsMigrated = false

    fun hasCredentials(site: SiteModel): Boolean = getCredentials(site) != null

    @Synchronized
    internal fun getCredentials(site: SiteModel): ApplicationPasswordCredentials? {
        val username = encryptedPreferences.getString(site.usernamePrefKey, null)
        val password = encryptedPreferences.getString(site.passwordPrefKey, null)
        val uuid = encryptedPreferences.getString(site.uuidPrefKey, null)

        return when {
            site.username != username && !site.isUsingWpComRestApi -> null
            username != null && password != null ->
                ApplicationPasswordCredentials(
                    userName = username,
                    password = password,
                    uuid = uuid
                )
            else -> null
        }
    }

    @Synchronized
    fun saveCredentials(site: SiteModel, credentials: ApplicationPasswordCredentials) {
        encryptedPreferences.edit()
            .putString(site.usernamePrefKey, credentials.userName)
            .putString(site.passwordPrefKey, credentials.password)
            .putString(site.uuidPrefKey, credentials.uuid)
            .apply()
    }

    @Synchronized
    fun deleteCredentials(site: SiteModel) {
        encryptedPreferences.edit()
            .remove(site.usernamePrefKey)
            .remove(site.passwordPrefKey)
            .remove(site.uuidPrefKey)
            .apply()
    }

    /**
     * The wp-admin password of a site logged in with site credentials. It's used to create application passwords,
     * and lives here rather than on [SiteModel] so that deleting it on logout takes it away from any copy of the
     * site that in-flight work still holds.
     */
    @Synchronized
    fun getSitePassword(site: SiteModel): String? {
        migrateLegacySitePasswordsIfNeeded()
        val username = encryptedPreferences.getString(site.siteUsernamePrefKey, null)
        return encryptedPreferences.getString(site.sitePasswordPrefKey, null)
            ?.takeIf { username != null && username == site.username }
    }

    fun hasSitePassword(site: SiteModel): Boolean = !getSitePassword(site).isNullOrEmpty()

    @Synchronized
    fun saveSitePassword(site: SiteModel, username: String, password: String) {
        migrateLegacySitePasswordsIfNeeded()
        encryptedPreferences.edit()
            .putString(site.siteUsernamePrefKey, username)
            .putString(site.sitePasswordPrefKey, password)
            .apply()
    }

    @Synchronized
    fun deleteSitePassword(site: SiteModel) {
        migrateLegacySitePasswordsIfNeeded()
        encryptedPreferences.edit()
            .remove(site.siteUsernamePrefKey)
            .remove(site.sitePasswordPrefKey)
            .apply()
    }

    /**
     * Moves the passwords that older versions kept in plain text in the sites table, without waiting for the first
     * access to them, which might never come while the application password keeps working.
     */
    @Synchronized
    fun migrateLegacySitePasswords() = migrateLegacySitePasswordsIfNeeded()

    /**
     * It also runs before any access to the stored passwords, so that deleting one can't be undone by a later
     * migration of its legacy copy.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun migrateLegacySitePasswordsIfNeeded() {
        if (areLegacySitePasswordsMigrated) return
        areLegacySitePasswordsMigrated = true

        try {
            val legacyPasswords = siteStorePersistence.getLegacySitePasswords()
            if (legacyPasswords.isEmpty()) return

            val editor = encryptedPreferences.edit()
            legacyPasswords.forEach { legacy ->
                val site = SiteModel().apply { url = legacy.url }
                if (legacy.username != null) {
                    editor.putString(site.siteUsernamePrefKey, legacy.username)
                        .putString(site.sitePasswordPrefKey, legacy.password)
                }
            }
            // Committed synchronously, the legacy copies are cleared right after
            editor.commit()
            siteStorePersistence.clearLegacySitePasswords()
            AppLog.d(AppLog.T.MAIN, "Moved ${legacyPasswords.size} site password(s) to the encrypted storage")
        } catch (e: Exception) {
            AppLog.e(AppLog.T.MAIN, "Failed to move the site passwords to the encrypted storage", e)
        }
    }

    private fun initEncryptedPrefs(): SharedPreferences {
        val keySpec = MasterKeys.AES256_GCM_SPEC
        val filename = "$applicationName-encrypted-prefs"

        fun createPrefs(): SharedPreferences {
            val masterKey = MasterKeys.getOrCreate(keySpec)
            return EncryptedSharedPreferences.create(
                filename,
                masterKey,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        fun deletePrefs() {
            context.deleteSharedPreferences(filename)
            with(KeyStore.getInstance("AndroidKeyStore")) {
                load(null)
                if (containsAlias(keySpec.keystoreAlias)) {
                    deleteEntry(keySpec.keystoreAlias)
                }
            }
        }

        // The documentation recommends excluding the file from auto backup, but since the file
        // is defined in an internal library, adding to the backup rules and maintaining them won't
        // be straightforward.
        // So instead, we use a destructive approach, if we can't decrypt the file after restoring it,
        // We simply delete it and create a new one.
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        return try {
            createPrefs()
        } catch (e: Exception) {
            // In case we can't decrypt the file after a backup, let's delete it
            AppLog.d(
                AppLog.T.MAIN,
                "Can't decrypt encrypted preferences, delete it and create new one"
            )
            deletePrefs()
            createPrefs()
        }
    }

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url).trim('/')

    private val SiteModel.usernamePrefKey
        get() = "$USERNAME_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.passwordPrefKey
        get() = "$PASSWORD_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.uuidPrefKey
        get() = "$UUID_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.siteUsernamePrefKey
        get() = "$SITE_USERNAME_PREFERENCE_KEY_PREFIX$domainName"

    private val SiteModel.sitePasswordPrefKey
        get() = "$SITE_PASSWORD_PREFERENCE_KEY_PREFIX$domainName"
}
