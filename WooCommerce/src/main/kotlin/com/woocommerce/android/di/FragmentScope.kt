package com.woocommerce.android.di

import dagger.releasablereferences.CanReleaseReferences
import javax.inject.Scope

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@CanReleaseReferences
@Scope
annotation class FragmentScope
