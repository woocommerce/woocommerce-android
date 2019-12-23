package com.woocommerce.android.ui.aztec

import com.woocommerce.android.di.FragmentScope
import com.woocommerce.android.ui.aztec.AztecModule.AztecEditorFragmentModule
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [
    AztecEditorFragmentModule::class
])
object AztecModule {
    @Module
    abstract class AztecEditorFragmentModule {
        @FragmentScope
        @ContributesAndroidInjector(modules = [AztecEditorModule::class])
        abstract fun aztecEditorFragment(): AztecEditorFragment
    }
}
