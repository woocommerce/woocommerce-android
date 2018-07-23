package com.woocommerce.android.di

import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import com.woocommerce.android.tools.SelectedSite
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Singleton

@Module
object MockedSelectedSiteModule {
    private var siteModel: SiteModel? = null

    fun setSiteModel(siteModel: SiteModel) {
        this.siteModel = siteModel
    }

    @JvmStatic
    @Provides
    @Singleton
    fun provideSelectedSite(context: Context): SelectedSite {
        val selectedSite = SelectedSite(context, mock())
        siteModel?.let {
            selectedSite.set(it)
        }
        return selectedSite
    }
}
