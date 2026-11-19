package com.github.intellij.plugins.mt4ij

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TemplatesFolderKindTest {

    @Test
    fun mainTemplatesFolderIsASourceFolder() {
        assertEquals(TemplatesFolderKind.SOURCE, TemplatesFolderKind.of("/work/app/src/main/java-templates", "java-templates"))
    }

    @Test
    fun testTemplatesFolderIsATestFolder() {
        assertEquals(TemplatesFolderKind.TEST, TemplatesFolderKind.of("/work/app/src/test/java-templates", "java-templates"))
    }

    @Test
    fun trailingSlashIsIgnored() {
        assertEquals(TemplatesFolderKind.SOURCE, TemplatesFolderKind.of("/work/app/src/main/java-templates/", "java-templates"))
    }

    @Test
    fun customNameIsRecognizedInsteadOfTheDefaultOne() {
        assertEquals(TemplatesFolderKind.SOURCE, TemplatesFolderKind.of("/work/app/src/main/tpl", "tpl"))
        assertNull(TemplatesFolderKind.of("/work/app/src/main/java-templates", "tpl"))
    }

    @Test
    fun folderOutsideSrcMainOrSrcTestIsNotATemplatesFolder() {
        assertNull(TemplatesFolderKind.of("/work/app/src/main/java/com/domain/java-templates", "java-templates"))
        assertNull(TemplatesFolderKind.of("/work/app/domain/java-templates", "java-templates"))
        assertNull(TemplatesFolderKind.of("/work/app/src/other/java-templates", "java-templates"))
    }

    @Test
    fun onlyWholePathSegmentsMatch() {
        assertNull(TemplatesFolderKind.of("/work/app/src/main/my-java-templates", "java-templates"))
        assertNull(TemplatesFolderKind.of("/work/app/src/main/java-templates-old", "java-templates"))
    }

    @Test
    fun blankNameMatchesNothing() {
        assertNull(TemplatesFolderKind.of("/work/app/src/main/", ""))
        assertNull(TemplatesFolderKind.of("/work/app/src/main/java-templates", "  "))
    }

    @Test
    fun kindTellsWhetherItIsATestFolder() {
        assertEquals(false, TemplatesFolderKind.SOURCE.isTest)
        assertEquals(true, TemplatesFolderKind.TEST.isTest)
    }
}
