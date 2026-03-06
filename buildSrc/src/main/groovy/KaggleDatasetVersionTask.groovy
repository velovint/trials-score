import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Checks the current file listing of a Kaggle dataset and writes an MD5 fingerprint
 * to a version file. Downstream download tasks declare this file as an @InputFile so
 * they are invalidated automatically when the dataset changes.
 */
abstract class KaggleDatasetVersionTask extends DefaultTask {

    @Input
    abstract Property<String> getDatasetSlug()

    @OutputFile
    abstract RegularFileProperty getVersionFile()

    @TaskAction
    void check() {
        def out = new ByteArrayOutputStream()
        project.exec {
            commandLine 'kaggle', 'datasets', 'files', datasetSlug.get()
            standardOutput = out
        }
        versionFile.get().asFile.text = out.toString().trim().md5()
        logger.lifecycle("Dataset version (${datasetSlug.get()}): ${versionFile.get().asFile.text}")
    }
}
