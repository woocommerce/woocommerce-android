/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.woocommerce.android.viewmodel

import androidx.annotation.IdRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.navigation.fragment.findNavController

/**
 * Returns a property delegate to access a
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated [ViewModel] scoped to a navigation graph present on the [NavController] back stack:
 * ```
 * class MyFragment : Fragment() {
 *     val viewmodel: MainViewModel by com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels(R.navigation.main)
 * }
 * ```
 *
 * This property can be accessed only after this NavGraph is on the NavController back stack,
 * and an attempt access prior to that will result in an IllegalArgumentException.
 *
 * ** WooCommerce edit **
 * The default implementation from the androidx library doesn't supply the `extrasProducer` argument
 * to the `createViewModelLazy` function, this breaks the state restoration when the scoped ViewModel
 * is recreated after a process death from a child Fragment, as its SavedState will be linked to the Fragment
 * itself and not the NavBackStackEntry.
 * The fix below is to supply the `extrasProducer` argument to the `createViewModelLazy` function.
 *
 * @param navGraphId ID of a NavGraph that exists on the [NavController] back stack
 */
@MainThread
@Suppress("MissingNullability") // Due to https://youtrack.jetbrains.com/issue/KT-39209
public inline fun <reified VM : ViewModel> Fragment.fixedHiltNavGraphViewModels(
    @IdRes navGraphId: Int
): Lazy<VM> {
    val backStackEntry by lazy {
        findNavController().getBackStackEntry(navGraphId)
    }
    val storeProducer: () -> ViewModelStore = {
        backStackEntry.viewModelStore
    }
    return createViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = storeProducer,
        extrasProducer = {
            backStackEntry.defaultViewModelCreationExtras
        },
        factoryProducer = {
            HiltViewModelFactory(requireActivity(), backStackEntry)
        }
    )
}
