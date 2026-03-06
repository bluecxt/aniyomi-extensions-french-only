/**
 * Precompiled [lib-multisrc.gradle.kts][Lib_multisrc_gradle] script plugin.
 *
 * @see Lib_multisrc_gradle
 */
public
class LibMultisrcPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Lib_multisrc_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
