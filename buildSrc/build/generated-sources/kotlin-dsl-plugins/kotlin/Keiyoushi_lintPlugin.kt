/**
 * Precompiled [keiyoushi.lint.gradle.kts][Keiyoushi_lint_gradle] script plugin.
 *
 * @see Keiyoushi_lint_gradle
 */
public
class Keiyoushi_lintPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Keiyoushi_lint_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
