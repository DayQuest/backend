package com.example.dayquest.Controller;

import com.example.dayquest.Service.UserService;
import com.example.dayquest.Service.VideoService;
import com.example.dayquest.model.Quest;
import com.example.dayquest.Service.QuestService;
import com.example.dayquest.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.dayquest.Repository.QuestRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    @Autowired
    private QuestRepository questRepository;
    private final UserService userService;
    public QuestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<Quest> getAllQuests() {
        return questRepository.findAll();
    }

    @PostMapping("/suggest")
    public ResponseEntity<Quest> suggestQuest(@RequestBody Quest quest) {
        Quest newQuest = questRepository.save(quest);
        return ResponseEntity.status(HttpStatus.CREATED).body(newQuest);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeQuest(@PathVariable Long id) {
        questRepository.findById(id).ifPresent(quest -> {
            quest.setLikes(quest.getLikes() + 1);
            questRepository.save(quest);
        });
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Void> dislikeQuest(@PathVariable Long id) {
        questRepository.findById(id).ifPresent(quest -> {
            quest.setDislikes(quest.getDislikes() + 1);
            questRepository.save(quest);
        });
        return ResponseEntity.ok().build();
    }
    @PostMapping("/getquest")
    public ResponseEntity<String> getQuest(@RequestBody Map<String, Long> request)
    {
        Long id = request.get("userId");
        User user = userService.getUserById(id);
        Quest quest = user.getDailyQuest();
        return ResponseEntity.ok(quest.getDescription());
    }
}

