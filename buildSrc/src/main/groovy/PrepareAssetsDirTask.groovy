import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Syncs an input directory into an output directory so it can be wired as a
 * generated asset source via {@code androidComponents.onVariants}.
 *
 * Use this as a typed bridge when the download task's {@code outputDir} is a
 * subdirectory of the desired asset root (e.g. {@code raw-pictures/raw/}), and
 * the asset path must include the subdirectory name (e.g. {@code raw/file.png}).
 */
abstract class PrepareAssetsDirTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract DirectoryProperty getInputDir()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void prepare() {
        project.sync {
            from inputDir
            into outputDir
        }
    }
}
