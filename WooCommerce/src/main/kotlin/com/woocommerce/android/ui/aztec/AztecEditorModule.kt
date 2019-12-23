package com.woocommerce.android.ui.aztec

import android.os.Bundle
import dagger.Module
import dagger.Provides

@Module
abstract class AztecEditorModule {
    @Module
    companion object {
        @JvmStatic
        @Provides
        fun provideDefaultArgs(fragment: AztecEditorFragment): Bundle? {
            return fragment.arguments
        }
    }
}
