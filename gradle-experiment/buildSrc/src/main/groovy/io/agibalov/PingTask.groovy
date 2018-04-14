import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import static io.agibalov.ShellService.shell

class PingTask extends DefaultTask {
    @Input
    @Option(option = "hostname", description = "IP address to ping")
    String hostname

    @TaskAction
    void ping() {
        shell("""ping ${hostname} -c3""")
    }
}
