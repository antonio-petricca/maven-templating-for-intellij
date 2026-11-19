package com.github.intellij.plugins.mt4ij.properties

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager

/** Where a property used in a template is defined. [target] is the pom element to navigate to, if any. */
class PropertyDefinition(val target: PsiElement?)

/** The properties available to the templates of a Maven module: its pom and parents, and the project model. */
object TemplateProperties {

    // Project model properties and the pom tag (below <project>) that holds their value
    private val MODEL_PROPERTIES = mapOf(
        "project.version"         to listOf("version"),
        "project.groupId"         to listOf("groupId"),
        "project.artifactId"      to listOf("artifactId"),
        "project.name"            to listOf("name"),
        "project.description"     to listOf("description"),
        "project.basedir"         to emptyList(),
        "project.build.directory" to listOf("build", "directory"),
    )

    // Values the module pom may leave out and take from its <parent> tag
    private val INHERITED_TAGS = setOf("groupId", "version")

    // Properties supplied by the system, the environment or Maven itself: never reported
    private val IGNORED_PREFIXES = listOf("env.", "java.", "os.", "user.", "settings.", "maven.", "project.", "pom.")

    fun isIgnored(name: String): Boolean =
        name == "basedir" || IGNORED_PREFIXES.any { name.startsWith(it) } || name in System.getProperties().stringPropertyNames()

    /** The pom of the module followed by the poms of its parents, nearest first. */
    fun pomChain(project: Project, mavenProject: MavenProject): List<XmlFile> {
        val manager = MavenProjectsManager.getInstance(project)
        val chain   = mutableListOf<XmlFile>()
        val visited = mutableSetOf<MavenProject>()

        var current: MavenProject? = mavenProject

        while (current != null && visited.add(current)) {
            pomOf(project, current.file)?.let { chain += it }
            current = current.parentId?.let { manager.findProject(it) }
        }

        return chain
    }

    fun modulePom(project: Project, mavenProject: MavenProject): XmlFile? = pomOf(project, mavenProject.file)

    /** Properties declared in `<properties>`, the nearest pom winning over its parents. */
    fun declared(project: Project, mavenProject: MavenProject): Map<String, XmlTag> {
        val result = linkedMapOf<String, XmlTag>()

        pomChain(project, mavenProject).forEach { pom ->
            declaredIn(pom).forEach { (name, tag) -> result.putIfAbsent(name, tag) }
        }

        return result
    }

    fun modelNames(): Set<String> = MODEL_PROPERTIES.keys

    /** Null when the property is unknown. */
    fun find(project: Project, mavenProject: MavenProject, name: String): PropertyDefinition? {
        declared(project, mavenProject)[name]?.let { return PropertyDefinition(it) }

        MODEL_PROPERTIES[name]?.let { return PropertyDefinition(modelTarget(pomChain(project, mavenProject), it)) }

        // Already resolved by the Maven model (profiles, parents, config files...): nothing to navigate to
        if (mavenProject.properties.containsKey(name)) {
            return PropertyDefinition(null)
        }

        return null
    }

    private fun modelTarget(chain: List<XmlFile>, path: List<String>): PsiElement? {
        val moduleRoot = chain.firstOrNull()?.rootTag ?: return null

        if (path.isEmpty()) {
            return moduleRoot
        }

        chain.forEachIndexed { index, pom ->
            val root = pom.rootTag ?: return@forEachIndexed

            subTag(root, path)?.let { return it }

            if (index == 0 && path.size == 1 && path[0] in INHERITED_TAGS) {
                root.findFirstSubTag("parent")?.findFirstSubTag(path[0])?.let { return it }
            }
        }

        // A value that is not written anywhere (for instance the build directory): point at the project tag
        return moduleRoot
    }

    private fun subTag(root: XmlTag, path: List<String>): XmlTag? =
        path.fold(root as XmlTag?) { tag, name -> tag?.findFirstSubTag(name) }

    private fun declaredIn(pom: XmlFile): Map<String, XmlTag> =
        CachedValuesManager.getCachedValue(pom) {
            val tags = pom.rootTag
                ?.findFirstSubTag("properties")
                ?.subTags
                ?.associateBy { it.name }
                ?: emptyMap()

            CachedValueProvider.Result.create(tags, pom)
        }

    private fun pomOf(project: Project, file: VirtualFile): XmlFile? =
        PsiManager.getInstance(project).findFile(file) as? XmlFile
}
