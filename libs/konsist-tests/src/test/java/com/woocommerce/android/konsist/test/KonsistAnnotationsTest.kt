package com.woocommerce.android.konsist.test

import android.app.Application
import androidx.fragment.app.Fragment
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.properties
import com.lemonappdev.konsist.api.ext.list.withoutAllParentsNamed
import com.lemonappdev.konsist.api.ext.list.withoutAllParentsOf
import com.lemonappdev.konsist.api.ext.list.withoutAnnotationOf
import com.lemonappdev.konsist.api.ext.list.withoutName
import com.lemonappdev.konsist.api.ext.provider.hasAnnotationOf
import com.lemonappdev.konsist.api.verify.assertFalse
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import javax.inject.Inject

class KonsistAnnotationsTest {
    @Test // Or suppress 'AppInitializer' class: @Suppress("konsist.no class should use field injection")
    fun `no class should use field injection`() {
        Konsist.scopeFromProject()
            .classes()
            .withoutAnnotationOf(HiltAndroidApp::class, AndroidEntryPoint::class, HiltAndroidTest::class)
            .withoutAllParentsOf(Application::class)
            .withoutAllParentsOf(Fragment::class)
            .withoutAllParentsNamed("BaseFragment")
            .withoutName("AppInitializer")
            .properties()
            .assertFalse { it.hasAnnotationOf<Inject>() }
    }
}
