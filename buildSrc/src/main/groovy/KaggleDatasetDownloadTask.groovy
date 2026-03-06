import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Downloads and unzips a Kaggle dataset into {@link #outputDir}.
 *
 * Cache invalidation is driven by {@link #versionFile}: pair this task with a
 * {@link KaggleDatasetVersionTask} and wire its output here so the download is
 * skipped when the remote dataset has not changed.
 */
@CacheableTask
abstract class KaggleDatasetDownloadTask extends DefaultTask {

    @Input
    abstract Property<String> getDatasetSlug()

    /** MD5 fingerprint produced by {@link KaggleDatasetVersionTask}. */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getVersionFile()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    void download() {
        def destDir = outputDir.get().asFile
        destDir.deleteDir()
        destDir.mkdirs()

        project.exec {
            commandLine 'kaggle', 'datasets', 'download',
                datasetSlug.get(), '--unzip', '--path', destDir.absolutePath
        }
        logger.lifecycle("Downloaded ${datasetSlug.get()} to ${destDir}")
    }
}
