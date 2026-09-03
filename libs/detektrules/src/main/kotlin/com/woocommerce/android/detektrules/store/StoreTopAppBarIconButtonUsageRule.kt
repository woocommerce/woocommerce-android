package com.woocommerce.android.detektrules.store

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList

class StoreTopAppBarIconButtonUsageRule(config: Config) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Material IconButton must not be used directly in Store top app bar actions.",
        Debt.FIVE_MINS
    )

    private var iconButtonNames: Set<String> = emptySet()
    private var topAppBarNames: Set<String> = emptySet()
    private var menuOwnerNames: Set<String> = emptySet()
    private var actionsScopeNames: Set<String> = emptySet()

    override fun visitKtFile(file: KtFile) {
        iconButtonNames = file.importedMaterialNames(ICON_BUTTON_NAME)
        topAppBarNames = file.importedCallableNames(TARGET_CALLABLES)
        menuOwnerNames = file.importedMaterialNames(DROP_DOWN_MENU_NAME) +
            file.importedCallableNames(setOf(WOO_OVERFLOW_MENU_CALLABLE))
        actionsScopeNames = file.importedCallableNames(setOf(TOP_APP_BAR_ACTIONS_SCOPE))

        super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (!expression.isMaterialIconButton() || !expression.isInsideTopAppBarActions()) return

        report(
            CodeSmell(
                issue,
                Entity.from(expression),
                "Use IconAction inside WooTopAppBar actions instead of Material IconButton."
            )
        )
    }

    private fun KtCallExpression.isMaterialIconButton(): Boolean =
        matchesCallable(MATERIAL_ICON_BUTTON_CALLABLES, iconButtonNames)

    /**
     * A qualified call only matches when its receiver spells out a target package, so an unrelated
     * `Other.WooTopAppBar(...)` is never matched through an import of the design-system `WooTopAppBar`.
     */
    private fun KtCallExpression.matchesCallable(callables: Set<String>, importedNames: Set<String>): Boolean {
        val qualifiedName = fullyQualifiedCallableName()
        if (qualifiedName != null) return qualifiedName in callables
        val calleeName = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
        return calleeName in importedNames
    }

    private fun KtCallExpression.isInsideTopAppBarActions(): Boolean {
        var ancestor = parent
        var isInsideMenuContent = false
        while (ancestor != null) {
            when (ancestor) {
                is KtLambdaExpression -> {
                    if (ancestor.isTopAppBarActions()) return !isInsideMenuContent
                    if (ancestor.isMenuContent()) isInsideMenuContent = true
                }

                is KtProperty -> {
                    if (ancestor.hasTopAppBarActionsScopeType()) return !isInsideMenuContent
                }

                is KtNamedFunction -> {
                    if (ancestor.hasTopAppBarActionsScopeReceiver()) return !isInsideMenuContent
                }
            }
            ancestor = ancestor.parent
        }
        return false
    }

    private fun KtLambdaExpression.isTopAppBarActions(): Boolean {
        val ownerCall = ownerCall() ?: return false
        if (!ownerCall.isTargetTopAppBarCall()) return false
        return isNamed(ACTIONS_ARGUMENT_NAME) || isPositionalActionsArgument()
    }

    /**
     * `actions` is declared last, but never before index [MIN_POSITIONAL_ACTIONS_INDEX] in any target overload, so a
     * positional composable `title` lambda is not mistaken for positional actions.
     */
    private fun KtLambdaExpression.isPositionalActionsArgument(): Boolean {
        if (parent is KtLambdaArgument) return true
        if (!isUnnamedTrailingArgument()) return false
        val argumentList = (parent as? KtValueArgument)?.parent as? KtValueArgumentList
        return (argumentList?.arguments?.size ?: 0) > MIN_POSITIONAL_ACTIONS_INDEX
    }

    private fun KtLambdaExpression.isMenuContent(): Boolean {
        val ownerCall = ownerCall() ?: return false
        if (!isNamed(CONTENT_ARGUMENT_NAME) && !isUnnamedTrailingArgument()) return false
        if (ownerCall.isOverflowActionCall()) return true
        return ownerCall.matchesCallable(MENU_OWNER_CALLABLES, menuOwnerNames)
    }

    private fun KtCallExpression.isOverflowActionCall(): Boolean {
        val calleeName = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName()
        return fullyQualifiedCallableName() == null && calleeName == OVERFLOW_ACTION_NAME
    }

    private fun KtLambdaExpression.ownerCall(): KtCallExpression? {
        val lambdaArgument = parent as? KtLambdaArgument
        if (lambdaArgument != null) {
            var ancestor = lambdaArgument.parent
            while (ancestor != null && ancestor !is KtCallExpression) {
                ancestor = ancestor.parent
            }
            return ancestor as? KtCallExpression
        }
        val valueArgument = parent as? KtValueArgument ?: return null
        return (valueArgument.parent as? KtValueArgumentList)?.parent as? KtCallExpression
    }

    private fun KtLambdaExpression.isNamed(argumentName: String): Boolean =
        (parent as? KtValueArgument)?.getArgumentName()?.asName?.identifier == argumentName

    /**
     * A lambda passed without an argument name only counts when it is the last argument of the call, so a positional
     * `title` lambda is never mistaken for the trailing `actions` or `content` lambda.
     */
    private fun KtLambdaExpression.isUnnamedTrailingArgument(): Boolean {
        if (parent is KtLambdaArgument) return true
        val valueArgument = parent as? KtValueArgument
        val argumentList = valueArgument?.parent as? KtValueArgumentList
        val ownerCall = argumentList?.parent as? KtCallExpression
        return ownerCall != null &&
            ownerCall.lambdaArguments.isEmpty() &&
            valueArgument.getArgumentName() == null &&
            argumentList.arguments.lastOrNull() == valueArgument
    }

    private fun KtCallExpression.isTargetTopAppBarCall(): Boolean =
        matchesCallable(TARGET_CALLABLES, topAppBarNames)

    private fun KtCallExpression.fullyQualifiedCallableName(): String? {
        val calleeName = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null
        val qualifiedCall = parent as? KtDotQualifiedExpression ?: return null
        if (qualifiedCall.selectorExpression != this) return null
        return "${qualifiedCall.receiverExpression.text}.$calleeName"
    }

    private fun KtNamedFunction.hasTopAppBarActionsScopeReceiver(): Boolean {
        val receiverType = receiverTypeReference?.text ?: return false
        return receiverType == TOP_APP_BAR_ACTIONS_SCOPE || receiverType in actionsScopeNames
    }

    private fun KtProperty.hasTopAppBarActionsScopeType(): Boolean {
        val declaredType = typeReference?.text ?: return false
        val scopeNames = actionsScopeNames + TOP_APP_BAR_ACTIONS_SCOPE
        return scopeNames.any { declaredType.contains("$it.(") }
    }

    private fun KtFile.importedMaterialNames(simpleName: String): Set<String> = buildSet {
        importDirectives.forEach { directive ->
            val path = directive.importPath?.pathStr ?: return@forEach
            MATERIAL_PACKAGES.forEach { materialPackage ->
                when (path) {
                    "$materialPackage.$simpleName" -> add(directive.aliasName ?: simpleName)
                    "$materialPackage.*" -> add(simpleName)
                }
            }
        }
    }

    private fun KtFile.importedCallableNames(callables: Set<String>): Set<String> = buildSet {
        callables.forEach { callable ->
            val callablePackage = callable.substringBeforeLast('.')
            val simpleName = callable.substringAfterLast('.')
            if (packageFqName.asString() == callablePackage) add(simpleName)
            importDirectives.forEach { directive ->
                when (directive.importPath?.pathStr) {
                    callable -> add(directive.aliasName ?: simpleName)
                    "$callablePackage.*" -> add(simpleName)
                }
            }
        }
    }

    private companion object {
        const val ACTIONS_ARGUMENT_NAME = "actions"
        const val CONTENT_ARGUMENT_NAME = "content"
        const val DROP_DOWN_MENU_NAME = "DropdownMenu"
        const val ICON_BUTTON_NAME = "IconButton"
        const val OVERFLOW_ACTION_NAME = "OverflowAction"
        const val MIN_POSITIONAL_ACTIONS_INDEX = 4
        const val WOO_OVERFLOW_MENU_CALLABLE =
            "com.woocommerce.android.ui.compose.designsystem.component.WooOverflowMenu"
        const val TOP_APP_BAR_ACTIONS_SCOPE =
            "com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBarActionsScope"

        val MATERIAL_PACKAGES = setOf(
            "androidx.compose.material",
            "androidx.compose.material3",
        )
        val TARGET_CALLABLES = setOf(
            "com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar",
        )
        val MENU_OWNER_CALLABLES = MATERIAL_PACKAGES.mapTo(mutableSetOf()) {
            "$it.$DROP_DOWN_MENU_NAME"
        } + WOO_OVERFLOW_MENU_CALLABLE
        val MATERIAL_ICON_BUTTON_CALLABLES = MATERIAL_PACKAGES.mapTo(mutableSetOf()) {
            "$it.$ICON_BUTTON_NAME"
        }
    }
}
