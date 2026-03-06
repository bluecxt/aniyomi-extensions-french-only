/**
 * Precompiled [lib-kotlin.gradle.kts][Lib_kotlin_gradle] script plugin.
 *
 * @see Lib_kotlin_gradle
 */
public
class LibKotlinPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Lib_kotlin_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
