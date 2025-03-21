package com.woocommerce.android.konsist.test

import androidx.lifecycle.ViewModel
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAllParentsNamed
import com.lemonappdev.konsist.api.ext.list.withAllParentsOf
import com.lemonappdev.konsist.api.ext.list.withName
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withoutName
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistTestsTest {
    @Test // Or suppress 'ScopedViewModel' class: @Suppress("konsist.every view model class has test")
    fun `every view model class has test`() {
        Konsist.scopeFromProduction()
            .classes()
            .withAllParentsOf(ViewModel::class)
            .plus(
                Konsist.scopeFromProject()
                    .classes()
                    .withAllParentsNamed("ScopedViewModel")
            )
            .withoutName("ScopedViewModel")
            .assertTrue { viewModelClass ->
                Konsist.scopeFromProject()
                    .classes()
                    .withName("${viewModelClass.name}Test")
                    .isNotEmpty()
            }
    }

    @Test
    fun `every use case class has test`() {
        Konsist.scopeFromProduction()
            .classes()
            .withNameEndingWith("UseCase")
            .assertTrue { useCaseClass ->
                Konsist.scopeFromProject()
                    .classes()
                    .withName("${useCaseClass.name}Test")
                    .isNotEmpty()
            }
    }

    @Test
    fun `every repository class has test`() {
        Konsist.scopeFromProduction()
            .classes()
            .withNameEndingWith("Repository")
            .assertTrue { repositoryClass ->
                Konsist.scopeFromProject()
                    .classes()
                    .withName("${repositoryClass.name}Test")
                    .isNotEmpty()
            }
    }

    @Test
    fun `test classes should have test subject named sut`() {
        Konsist.scopeFromTest()
            .classes()
            .assertTrue {
                val type = it.name.removeSuffix("Test")
                val sut = it
                    .properties()
                    .firstOrNull { property -> property.name == "sut" }
                sut != null && (sut.type?.name == type || sut.text.contains("$type("))
            }
    }

    @Test
    fun `classes with 'test' annotation should have 'test' suffix`() {
        Konsist.scopeFromSourceSet("test")
            .classes()
            .filter {
                it.functions().any { func -> func.hasAnnotationOf(Test::class) }
            }
            .assertTrue { it.hasNameEndingWith("Test") }
    }
}
