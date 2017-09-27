package me.loki2302;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RestController
    public static class ApiController {
        @Autowired
        private NoteRepository noteRepository;

        @GetMapping("/hello")
        public Map<String, String> hello() {
            return Collections.singletonMap("message", "Hello there " + new Date() + "!!!");
        }

        @GetMapping("/items")
        public List<Note> getItemCount() {
            Note note = new Note();
            note.text = String.format("Note %s %s", new Date(), UUID.randomUUID());
            noteRepository.save(note);
            return noteRepository.findAll();
        }
    }
}
