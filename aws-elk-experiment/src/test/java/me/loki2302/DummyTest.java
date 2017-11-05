package me.loki2302;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class DummyTest {
    @Autowired
    private AwsHandler.DummyService dummyService;

    @Test
    public void dummy() {
        dummyService.doSomething();
    }
}
