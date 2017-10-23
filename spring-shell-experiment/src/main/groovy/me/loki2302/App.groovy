package me.loki2302

import com.amazonaws.services.cloudformation.AmazonCloudFormation
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.model.DescribeStacksResult
import org.jline.reader.LineReader
import org.jline.reader.Parser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.shell.InputProvider
import org.springframework.shell.Shell
import org.springframework.shell.jline.DefaultShellApplicationRunner
import org.springframework.shell.jline.FileInputProvider
import org.springframework.shell.jline.PromptProvider
import org.springframework.shell.standard.ShellCommandGroup
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.web.client.RestTemplate
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessResult
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream

import java.util.concurrent.TimeoutException
import java.util.stream.Collectors

@SpringBootApplication
class App {
    static void main(String[] args) {
        SpringApplication.run(App.class, args)
    }

    @Bean
    AppProperties appProperties() {
        return new AppProperties(
                region: 'us-east-1',
                stackName: 'dummy1',
                bucketName: 'weiroiweur23')
    }

    @Bean
    @Autowired
    ApplicationRunner applicationRunner(
            LineReader lineReader,
            PromptProvider promptProvider,
            Parser parser,
            Shell shell) {

        return new MyApplicationRunner(lineReader, promptProvider, parser, shell)
    }

    static class MyApplicationRunner implements ApplicationRunner {
        private final LineReader lineReader
        private final PromptProvider promptProvider
        private final Parser parser
        private final Shell shell

        MyApplicationRunner(
                LineReader lineReader,
                PromptProvider promptProvider,
                Parser parser,
                Shell shell) {

            this.lineReader = lineReader
            this.promptProvider = promptProvider
            this.parser = parser
            this.shell = shell
        }

        @Override
        void run(ApplicationArguments args) throws Exception {
            boolean hasCommandLineArgs = args.getSourceArgs().length > 0
            if(hasCommandLineArgs) {
                String command = Arrays.stream(args.getSourceArgs())
                        .map({s -> s.contains(" ") ? "\"" + s + "\"" : s})
                        .collect(Collectors.joining(" "))

                Reader reader = new StringReader(command)
                FileInputProvider inputProvider = new FileInputProvider(reader, parser)
                shell.run(inputProvider)

                return
            }

            InputProvider inputProvider = new DefaultShellApplicationRunner.JLineInputProvider(
                    lineReader,
                    promptProvider)
            shell.run(inputProvider)
        }
    }

    static class AppProperties {
        String region
        String stackName
        String bucketName
    }

    @ShellComponent
    @ShellCommandGroup("Test Commands")
    static class TestCommands {
        private final static Logger LOGGER = LoggerFactory.getLogger(TestCommands.class)

        @Autowired
        private AppProperties appProperties

        @ShellMethod(key = "ping", value = "Ping the stack")
        void ping() {
            AmazonCloudFormation amazonCloudFormation = AmazonCloudFormationClientBuilder.defaultClient()
            DescribeStacksResult describeStacksResult = amazonCloudFormation.describeStacks(new DescribeStacksRequest()
                .withStackName(appProperties.stackName))
            String websiteUrl = describeStacksResult.stacks[0].outputs
                    .find { it.outputKey.equals('WebsiteURL') }
                    .outputValue
            LOGGER.info("WebsiteURL is {}", websiteUrl)

            RestTemplate restTemplate = new RestTemplate()
            String s = restTemplate.getForObject(websiteUrl, String.class)
            LOGGER.info("Content is {}", s)
        }
    }

    @ShellComponent
    @ShellCommandGroup("Deployment Commands")
    static class DeploymentCommands {
        private final static Logger LOGGER = LoggerFactory.getLogger(DeploymentCommands.class)

        @Autowired
        private AppProperties appProperties

        @ShellMethod(key = "deploy", value = "Create or update the stack")
        void deploy() {
            shell("""aws cloudformation deploy \
                --template-file cf.yml \
                --stack-name ${appProperties.stackName} \
                --capabilities CAPABILITY_IAM \
                --region ${appProperties.region} \
                --parameter-overrides \
                MyBucketName=${appProperties.bucketName}
            """)

            shell("""aws s3 cp public s3://${appProperties.bucketName} --recursive --acl public-read""")
        }

        @ShellMethod(key = "undeploy", value = "Delete the stack")
        void undeploy() {
            shell("""aws s3 rm s3://${appProperties.bucketName} --recursive""")
            shell("""aws cloudformation delete-stack \
                --stack-name ${appProperties.stackName} \
                --region ${appProperties.region}
            """)
            shell("""aws cloudformation wait stack-delete-complete \
                --stack-name ${appProperties.stackName} \
                --region ${appProperties.region}
            """)
        }

        private ProcessResult shell(String command) {
            try {
                return new ProcessExecutor()
                        .command("bash", "-c", command)
                        .readOutput(true)
                        .redirectOutputAlsoTo(Slf4jStream.of(LOGGER).asInfo())
                        .execute()
            } catch (IOException e) {
                throw new RuntimeException(e)
            } catch (InterruptedException e) {
                throw new RuntimeException(e)
            } catch (TimeoutException e) {
                throw new RuntimeException(e)
            }
        }
    }
}
