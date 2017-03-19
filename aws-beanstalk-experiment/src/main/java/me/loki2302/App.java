package me.loki2302;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RestController
    public static class DummyController {
        @Autowired
        private EventRepository eventRepository;

        @RequestMapping(produces = "text/plain")
        public String dummy() {
            Event event = new Event();
            event.date = new Date();
            eventRepository.save(event);

            Page<Event> page = eventRepository.findAll(new PageRequest(0, 20, Sort.Direction.DESC, "date"));

            StringBuilder sb = new StringBuilder();
            page.getContent().forEach(i -> sb.append(String.format("id=%d, date=%s\n", i.id, i.date)));

            return "Spring Boot says hi!\n" + sb.toString();
        }
    }
}
