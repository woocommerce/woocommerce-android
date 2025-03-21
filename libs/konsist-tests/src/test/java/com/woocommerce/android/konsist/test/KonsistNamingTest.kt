package com.woocommerce.android.konsist.test

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllParentsNamed
import com.lemonappdev.konsist.api.ext.list.withAllParentsOf
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistNamingTest {
    @Test
    fun `android activity class name ends with 'activity'`() {
        Konsist.scopeFromProject()
            .classes()
            .withAllParentsOf(AppCompatActivity::class)
            .assertTrue { it.name.endsWith("Activity") }
    }

    @Test
    fun `android fragment class name ends with 'fragment'`() {
        Konsist.scopeFromProject()
            .classes()
            .withAllParentsOf(Fragment::class)
            .plus(
                Konsist.scopeFromProject()
                    .classes()
                    .withAllParentsNamed("BaseFragment")
            )
            .assertTrue { it.name.endsWith("Fragment") }
    }

    @Test
    fun `android view model class name ends with 'viewmodel'`() {
        Konsist.scopeFromProject()
            .classes()
            .withAllParentsOf(ViewModel::class)
            .plus(
                Konsist.scopeFromProject()
                    .classes()
                    .withAllParentsNamed("ScopedViewModel")
            )
            .assertTrue { it.name.endsWith("ViewModel") }
    }
}
