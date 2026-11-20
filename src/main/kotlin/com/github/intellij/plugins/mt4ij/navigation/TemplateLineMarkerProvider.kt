package com.github.intellij.plugins.mt4ij.navigation

import com.github.intellij.plugins.mt4ij.Bundle
import com.github.intellij.plugins.mt4ij.TemplateLocator
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiManager

/** Gutter icon on a top-level class, leading from a template to its generated source and back. */
class TemplateLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = Bundle.message("mt4ij.navigation.linemarker.name")

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (!isTopLevelClassName(element)) {
            return
        }

        val file = element.containingFile.virtualFile ?: return
        val counterpart = TemplateLocator.counterpartOf(element.project, file) ?: return
        val target = PsiManager.getInstance(element.project).findFile(counterpart.file) ?: return

        result.add(
            NavigationGutterIconBuilder.create(counterpart.gutterIcon)
                .setTarget(target)
                .setTooltipText(counterpart.tooltipText)
                .setPopupTitle(counterpart.actionText)
                .createLineMarkerInfo(element)
        )
    }

    private fun isTopLevelClassName(element: PsiElement): Boolean {
        if (element !is PsiIdentifier) {
            return false
        }

        val psiClass = element.parent as? PsiClass ?: return false

        return psiClass.nameIdentifier === element && psiClass.containingClass == null
    }
}
