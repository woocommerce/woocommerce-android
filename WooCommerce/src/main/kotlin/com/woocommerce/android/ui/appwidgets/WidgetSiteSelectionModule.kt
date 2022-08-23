package com.woocommerce.android.ui.widgets

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel
import com.woocommerce.android.viewmodel.ViewModelKey
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap

@Module
abstract class WidgetSiteSelectionModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(): Bundle? {
            return null
        }

        @JvmStatic
        @Provides
        fun provideSavedStateRegistryOwner(fragment: WidgetSiteSelectionFragment): SavedStateRegistryOwner {
            return fragment.findNavController().getBackStackEntry(R.id.nav_graph_today_widget)
        }
    }
    @Binds
    @IntoMap
    @ViewModelKey(TodayWidgetConfigureViewModel::class)
    abstract fun bindFactory(factory: TodayWidgetConfigureViewModel.Factory): ViewModelAssistedFactory<out ViewModel>
}
