package com.woocommerce.android.ui.designsystem

import android.content.Context
import androidx.fragment.app.Fragment
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

internal class DesignSystemModeResolver(
    private val featureFlagRepository: FeatureFlagRepository,
) {
    fun resolveDefaultMode(): DesignSystemMode =
        if (featureFlagRepository.isEnabled(FeatureFlag.NEW_DESIGN_SYSTEM)) {
            DesignSystemMode.DESIGN_SYSTEM
        } else {
            DesignSystemMode.LEGACY
        }
}

fun Fragment.defaultDesignSystemMode(): DesignSystemMode {
    val featureFlagRepository = EntryPoints.get(
        requireContext().applicationContext,
        DesignSystemFeatureFlagEntryPoint::class.java
    ).featureFlagRepository()

    return DesignSystemModeResolver(featureFlagRepository).resolveDefaultMode()
}

fun Context.defaultDesignSystemMode(): DesignSystemMode {
    val featureFlagRepository = EntryPoints.get(
        applicationContext,
        DesignSystemFeatureFlagEntryPoint::class.java
    ).featureFlagRepository()

    return DesignSystemModeResolver(featureFlagRepository).resolveDefaultMode()
}

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface DesignSystemFeatureFlagEntryPoint {
    fun featureFlagRepository(): FeatureFlagRepository
}
