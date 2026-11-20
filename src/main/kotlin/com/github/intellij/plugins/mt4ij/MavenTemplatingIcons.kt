package com.github.intellij.plugins.mt4ij

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Icons of the plugin; the dark variants are picked up from the `_dark` files by the IDE. */
object MavenTemplatingIcons {

    /** The icon of the plugin, 16x16, for its menus. */
    @JvmField
    val Plugin: Icon = IconLoader.getIcon("/icons/mavenTemplating.svg", MavenTemplatingIcons::class.java)

    /** Shown on a template: leads to the generated source. */
    @JvmField
    val GoToGenerated: Icon = IconLoader.getIcon("/icons/goToGenerated.svg", MavenTemplatingIcons::class.java)

    /** Shown on a generated source: leads back to the template. */
    @JvmField
    val GoToTemplate: Icon = IconLoader.getIcon("/icons/goToTemplate.svg", MavenTemplatingIcons::class.java)

    /** 12x12 variant of [GoToGenerated], for the gutter. */
    @JvmField
    val GoToGeneratedGutter: Icon = IconLoader.getIcon("/icons/goToGeneratedGutter.svg", MavenTemplatingIcons::class.java)

    /** 12x12 variant of [GoToTemplate], for the gutter. */
    @JvmField
    val GoToTemplateGutter: Icon = IconLoader.getIcon("/icons/goToTemplateGutter.svg", MavenTemplatingIcons::class.java)
}
