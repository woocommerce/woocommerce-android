package org.wordpress.android.fluxc.module

import android.content.Context
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.persistence.SitePluginDao
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase.Companion.buildDb
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeObjectivesDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeTargetingDao
import org.wordpress.android.fluxc.persistence.dao.ListDao
import org.wordpress.android.fluxc.persistence.dao.NotificationDao
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao
import org.wordpress.android.fluxc.persistence.dao.WPSiteSettingsDao
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import javax.inject.Singleton

@Module
class DatabaseModule {
    @Singleton
    @Provides
    fun provideDatabase(context: Context): WPAndroidDatabase {
        return buildDb(context)
    }

    @Singleton
    @Provides
    fun provideFeatureFlagConfigDao(wpAndroidDatabase: WPAndroidDatabase): FeatureFlagConfigDao {
        return wpAndroidDatabase.featureFlagConfigDao()
    }

    @Singleton
    @Provides
    fun provideDomainsDao(wpAndroidDatabase: WPAndroidDatabase): DomainDao {
        return wpAndroidDatabase.domainDao()
    }

    @Singleton
    @Provides
    fun provideBlazeCampaignsDao(wpAndroidDatabase: WPAndroidDatabase): BlazeCampaignsDao {
        return wpAndroidDatabase.blazeCampaignsDao()
    }

    @Singleton
    @Provides
    fun provideBlazeTargetingDao(wpAndroidDatabase: WPAndroidDatabase): BlazeTargetingDao {
        return wpAndroidDatabase.blazeTargetingDao()
    }

    @Singleton
    @Provides
    fun provideBlazeObjectivesDao(wpAndroidDatabase: WPAndroidDatabase): BlazeObjectivesDao {
        return wpAndroidDatabase.blazeObjectivesDao()
    }

    @Provides
    internal fun provideListDao(wpAndroidDatabase: WPAndroidDatabase): ListDao {
        return wpAndroidDatabase.listDao()
    }

    @Provides
    internal fun provideWhatsNewDao(wpAndroidDatabase: WPAndroidDatabase): WhatsNewDao {
        return wpAndroidDatabase.whatsNewDao()
    }

    @Provides
    internal fun provideNotificationDao(wpAndroidDatabase: WPAndroidDatabase): NotificationDao {
        return wpAndroidDatabase.notificationDao()
    }

    @Provides
    fun provideSitePluginDao(wpAndroidDatabase: WPAndroidDatabase): SitePluginDao {
        return wpAndroidDatabase.sitePluginDao()
    }

    @Provides
    fun provideWPSiteSettingsDao(wpAndroidDatabase: WPAndroidDatabase): WPSiteSettingsDao {
        return wpAndroidDatabase.wpSiteSettingsDao()
    }
}
