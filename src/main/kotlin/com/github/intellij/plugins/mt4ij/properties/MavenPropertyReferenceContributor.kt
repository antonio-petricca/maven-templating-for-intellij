package com.github.intellij.plugins.mt4ij.properties

import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

/** Adds a property reference for each `${name}` in the string literals and comments of the template files. */
class MavenPropertyReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val provider = object : PsiReferenceProvider() {
            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
                MavenPropertyReference.allIn(element).toTypedArray()
        }

        registrar.registerReferenceProvider(psiElement(PsiLiteralExpression::class.java), provider)
        registrar.registerReferenceProvider(psiElement(PsiComment::class.java).withLanguage(JavaLanguage.INSTANCE), provider)
    }
}
