package io.agibalov

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

class ShellService {
    private final static Logger LOGGER = LoggerFactory.getLogger(ShellService)

    static shell(String command) {
        LOGGER.warn('Running: {}', command)
        return new ProcessExecutor()
                .command("bash", "-c", command)
                .readOutput(true)
                .redirectOutputAlsoTo(Slf4jStream.of(LOGGER).asWarn())
                .execute()
    }
}
