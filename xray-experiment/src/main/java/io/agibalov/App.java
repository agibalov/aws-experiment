package io.agibalov;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.spring.aop.AbstractXRayInterceptor;
import com.amazonaws.xray.spring.aop.XRayEnabled;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Instant;
import java.util.Map;

@SpringBootApplication
@EnableAspectJAutoProxy
public class App {
    public static void main(String[] args) {
        new SpringApplicationBuilder(App.class).bannerMode(Banner.Mode.OFF).run(args);
    }

    @Bean
    public DummyService dummyService(JdbcTemplate jdbcTemplate) {
        return new DummyService(jdbcTemplate);
    }

    @Bean
    public DummyController dummyController(DummyService dummyService) {
        return new DummyController(dummyService);
    }

    @Bean
    public XRayInspector xRayInspector() {
        return new XRayInspector();
    }

    @Aspect
    public class XRayInspector extends AbstractXRayInterceptor {
        @Override
        protected Map<String, Map<String, Object>> generateMetadata(
                ProceedingJoinPoint proceedingJoinPoint,
                Subsegment subsegment) {

            return super.generateMetadata(proceedingJoinPoint, subsegment);
        }

        @Override
        @Pointcut("@within(com.amazonaws.xray.spring.aop.XRayEnabled)")
        public void xrayEnabledClasses() {
        }
    }

    @RequestMapping
    public static class DummyController {
        private final DummyService dummyService;

        public DummyController(DummyService dummyService) {
            this.dummyService = dummyService;
        }

        @GetMapping
        public ResponseEntity<?> hello() {
            try (Subsegment subsegment = AWSXRay.beginSubsegment("DummyControllerHandmade1")) {
                subsegment.putMetadata("omg", "qwerty");
                dummyService.doSomething();
                return ResponseEntity.ok(String.format("Hello %s", Instant.now()));
            } finally {
                AWSXRay.endSegment();
            }
        }
    }

    @XRayEnabled
    public static class DummyService {
        private final JdbcTemplate jdbcTemplate;

        public DummyService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @SneakyThrows
        public void doSomething() {
            try (Subsegment subsegment = AWSXRay.beginSubsegment("DummyServiceHandmade1")) {
                jdbcTemplate.queryForObject("select 1 + 1", Integer.class);
            }

            try (Subsegment subsegment = AWSXRay.beginSubsegment("DummyServiceHandmade2")) {
                jdbcTemplate.queryForObject("select 2 + 2", Integer.class);
            }

            try (Subsegment subsegment = AWSXRay.beginSubsegment("DummyServiceHandmade3")) {
                subsegment.putMetadata("hello", "world");
                Thread.sleep(10);
            }
        }
    }
}
