package com.woocommerce.android.detektrules.common

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.error.ErrorUtils

/**
 * Flags string-interpolating a whole object whose type (or, for a sealed type, any of its subclasses)
 * carries a function-type (lambda / function-reference) property.
 *
 * Why: stringifying such an object invokes its generated `toString()`, which renders the lambda through
 * `FunctionReference.toString()` -> kotlin-reflect. Under R8 full mode kotlin-reflect fails to resolve the
 * members ("no members found") and the app crashes at runtime — release builds only. Interpolate a field
 * or `x::class.simpleName` instead of the whole object.
 *
 * This rule is DETERMINISTIC: it resolves the actual type of every interpolated expression (requires type
 * resolution), so it catches the dangerous sites regardless of variable name and does not flag whole-object
 * interpolations of types that have no function-type property.
 */
@RequiresTypeResolution
class StringifyLambdaBearingObjectRule(config: Config) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "Stringifying an object whose type has a function-type (lambda) property triggers a reflective " +
            "toString() that crashes under R8 full mode. Interpolate a field or ::class.simpleName instead.",
        Debt.FIVE_MINS
    )

    override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
        super.visitStringTemplateExpression(expression)
        if (bindingContext == BindingContext.EMPTY) return

        expression.entries
            .filterIsInstance<KtStringTemplateEntryWithExpression>()
            .forEach { entry ->
                val interpolated = entry.expression ?: return@forEach
                val type = resolvedType(interpolated) ?: return@forEach
                if (rendersLambdaReflectively(type, mutableSetOf())) {
                    report(
                        CodeSmell(
                            issue,
                            Entity.from(interpolated),
                            "'${interpolated.text}' is stringified whole; its type carries a function-type " +
                                "property, so its toString() reflects on the lambda and can crash under R8. " +
                                "Interpolate a field or '${interpolated.text}::class.simpleName' instead."
                        )
                    )
                }
            }
    }

    // Prefer the expression's own type (it reflects smart-casts), else the referenced variable's declared
    // type, else — for a `when`-subject whose declared type can't resolve (an incomplete binding context) —
    // the type implied by the enclosing `when`'s `is`-branches, whose classifiers still resolve.
    private fun resolvedType(expression: KtExpression): KotlinType? {
        bindingContext.getType(expression)?.takeUnless { it.isResolutionFailure() }?.let { return it }

        (expression as? KtReferenceExpression)
            ?.let { bindingContext[BindingContext.REFERENCE_TARGET, it] as? VariableDescriptor }
            ?.type?.takeUnless { it.isResolutionFailure() }?.let { return it }

        return whenSubjectFallbackType(expression)
    }

    /**
     * Recover an unresolved `when`-subject's type from the branch conditions, whose `is`-pattern classifiers
     * resolve even when the subject's declared type does not. In an `is`-branch the subject is that guard
     * type; in `else` it can be any unhandled subclass, so use the sealed root.
     */
    private fun whenSubjectFallbackType(expression: KtExpression): KotlinType? {
        val reference = expression as? KtNameReferenceExpression ?: return null
        val whenExpression = expression.parents.filterIsInstance<KtWhenExpression>().firstOrNull() ?: return null
        if (reference.getReferencedName() != whenExpression.subjectVariable?.name) return null

        val containingEntry = expression.parents.filterIsInstance<KtWhenEntry>().firstOrNull()
        return containingEntry?.isPatternTypes().orEmpty().firstOrNull()
            ?: whenExpression.entries
                .flatMap { it.isPatternTypes() }
                .firstNotNullOfOrNull { it.sealedSupertype() }
    }

    private fun KtWhenEntry.isPatternTypes(): List<KotlinType> =
        conditions.filterIsInstance<KtWhenConditionIsPattern>()
            .mapNotNull { it.typeReference?.let { ref -> bindingContext[BindingContext.TYPE, ref] } }
            .filterNot { it.isResolutionFailure() }

    private fun KotlinType.sealedSupertype(): KotlinType? =
        constructor.supertypes.firstOrNull {
            (it.constructor.declarationDescriptor as? ClassDescriptor)?.modality == Modality.SEALED
        }

    private fun KotlinType.isResolutionFailure(): Boolean = ErrorUtils.containsErrorType(this)

    /**
     * A type renders a lambda through its generated toString() when it is itself a function type, is a
     * collection/map/array whose elements are lambdas, is a data class with a function-type property, or
     * (for a sealed type) any subclass does — since the static type at an interpolation site is often the
     * sealed root while the runtime value is a leaf.
     *
     * The property check is gated on [ClassDescriptor.isData]: only data classes generate a toString() that
     * renders their properties. A non-data class inherits Object's identity toString() and never touches its
     * lambda, so flagging it would be a false positive. Subclass recursion stays ungated — a non-data sealed
     * root can still have data-class subclasses that do render a lambda.
     */
    private fun rendersLambdaReflectively(type: KotlinType, visited: MutableSet<ClassDescriptor>): Boolean {
        if (type.isFunctionType || type.isSuspendFunctionType || type.rendersLambdaInTypeArguments(visited)) {
            return true
        }
        val descriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return false
        if (!visited.add(descriptor)) return false

        val ownPropertyRendersLambda = descriptor.isData &&
            descriptor.unsubstitutedPrimaryConstructor?.valueParameters.orEmpty()
                .any { rendersLambdaReflectively(it.type, visited) }
        return ownPropertyRendersLambda ||
            subclassTypes(descriptor).any { rendersLambdaReflectively(it, visited) }
    }

    // Arrays and any Collection/Iterable/Map render their elements in toString() (`[Function0]`,
    // `{k=Function0}`), so a lambda nested in one still crashes; unrelated generic wrappers such as
    // Comparator inherit an identity toString() and do not, so their type arguments are ignored.
    private fun KotlinType.rendersLambdaInTypeArguments(visited: MutableSet<ClassDescriptor>): Boolean =
        rendersContentsInToString() &&
            arguments.any { !it.isStarProjection && rendersLambdaReflectively(it.type, visited) }

    private fun KotlinType.rendersContentsInToString(): Boolean {
        if (KotlinBuiltIns.isArray(this)) return true
        val descriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return false
        return descriptor.getAllSuperClassifiers().any { it.fqNameSafe.asString() in COLLECTION_LIKE_FQ_NAMES }
    }

    /**
     * Subclasses of a sealed type. Prefer the descriptor's [ClassDescriptor.sealedSubclasses], but detekt's
     * binding context does not always populate it, so fall back to discovering same-file subclasses via PSI.
     * Only sealed types reach that fallback: for any non-sealed class [ClassDescriptor.sealedSubclasses] is
     * empty and there is nothing to discover, so we return early and skip the PSI traversal entirely.
     */
    private fun subclassTypes(descriptor: ClassDescriptor): List<KotlinType> {
        val fromDescriptor = descriptor.sealedSubclasses
        if (fromDescriptor.isNotEmpty()) return fromDescriptor.map { it.defaultType }
        if (descriptor.modality != Modality.SEALED) return emptyList()

        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtClassOrObject
            ?: return emptyList()
        val name = descriptor.name.asString()
        return declaration.containingKtFile
            .collectDescendantsOfType<KtClassOrObject> { candidate ->
                candidate != declaration && candidate.superTypeListEntries.any { entry ->
                    entry.typeReference?.text?.substringBefore('<')?.trim()?.substringAfterLast('.') == name
                }
            }
            .mapNotNull { bindingContext[BindingContext.CLASS, it]?.defaultType }
    }

    companion object {
        private val COLLECTION_LIKE_FQ_NAMES = setOf(
            "kotlin.collections.Iterable",
            "kotlin.collections.Map",
        )
    }
}
