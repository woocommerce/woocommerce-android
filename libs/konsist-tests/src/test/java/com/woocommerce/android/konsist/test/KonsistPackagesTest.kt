package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.ext.list.withoutName
import com.lemonappdev.konsist.api.ext.list.withoutSourceSet
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistPackagesTest {
    @Test
    fun `files in 'extensions' package must have name ending with 'ext'`() {
        Konsist.scopeFromProject()
            .files
            .withPackage("..extensions..")
            .withoutSourceSet("test")
            .assertTrue { it.hasNameEndingWith("Ext") }
    }

    @Test // Detekt: InvalidPackageDeclaration (https://detekt.dev/docs/rules/naming/#invalidpackagedeclaration)
    fun `package name must match file path`() {
        Konsist.scopeFromProject()
            .packages
            .withoutName("org.wordpress.android.util.config")
            .assertTrue { it.hasMatchingPath }
    }

    @Test
    fun `classes with 'usecase' suffix should reside in 'usecases' package`() {
        Konsist.scopeFromProject()
            .classes()
            .withNameEndingWith("UseCase")
            .assertTrue { it.resideInPackage("..usecases..") }
    }
}
