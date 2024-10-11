package com.example.dayquest.quest;

import com.example.dayquest.user.UserService;
import com.example.dayquest.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private static final Set<Pattern> prohibitedPatterns;

    static {
        prohibitedPatterns = new HashSet<>();
        // Gewalt und Aggression
        prohibitedPatterns.add(Pattern.compile("\\bkill\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bpisse\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bpissen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bcock\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmurder\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\battack\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bstab\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bshoot\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbomb\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bexplode\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbeat\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bassault\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\btorture\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bhurt\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bharm\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\btöten\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bermorden\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bangreifen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\berstechen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\berschießen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbombe\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bexplodieren\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bschlagen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bübersetzen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bfoltern\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bverletzen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bschaden\\b", Pattern.CASE_INSENSITIVE));

        // Selbstverletzung und Suizid
        prohibitedPatterns.add(Pattern.compile("\\bsuicide\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bself[- ]?harm\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bcut\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\boverdose\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bpoison\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsuffocate\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdrown\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsuizid\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bselbstverletzung\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\britzen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\büberdosis\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\berhängen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bvergiften\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bersticken\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bertrinken\\b", Pattern.CASE_INSENSITIVE));

        // Drogen und Sucht
        prohibitedPatterns.add(Pattern.compile("\\bdrugs?\\b", Pattern.CASE_INSENSITIVE));  // Abdeckt: drug, drugs
        prohibitedPatterns.add(Pattern.compile("\\bcocaine\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bheroin\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmeth\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bcrack\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\boverdose\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\binject\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\baddiction\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\babuse\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsniff\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdrogen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bkokain\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bheroin\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmeth\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bcrack\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\büberdosis\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\binjizieren\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsucht\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmissbrauch\\b", Pattern.CASE_INSENSITIVE));

        // Diskriminierung und Hassrede
        prohibitedPatterns.add(Pattern.compile("\\bracist\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bhate\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bslur\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdiscrimination\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbigotry\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bnazi\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bfascist\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\blynch\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\brassist\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bhass\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbeleidigung\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdiskriminierung\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbigotterie\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bnazi\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bfaschist\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\blynchen\\b", Pattern.CASE_INSENSITIVE));

        // Gefährliche Handlungen
        prohibitedPatterns.add(Pattern.compile("\\bburn\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\barson\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsteal\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\brob\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bvandalize\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdestroy\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsabotage\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bhijack\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bkidnap\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bransom\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbrennen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbrandstiftung\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bstehlen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\brauben\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bvandalismus\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bzerstören\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bsabotieren\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bentführen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\berpressung\\b", Pattern.CASE_INSENSITIVE));

        // Sexuelle Gewalt
        prohibitedPatterns.add(Pattern.compile("\\brape\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmolest\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bassault\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bharass\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\babuse\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\brape\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bvergewaltigen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmissbrauchen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bbelästigen\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bmisshandeln\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bterror\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bjihad\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bextremism\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bisis\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bterrorismus\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdschihad\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bextremismus\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bisis\\b", Pattern.CASE_INSENSITIVE));

        // Gefährliche Herausforderungen
        prohibitedPatterns.add(Pattern.compile("\\bdare\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bhazard\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\brisk\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bdanger\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bcock\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bherausforderung\\b", Pattern.CASE_INSENSITIVE));
        prohibitedPatterns.add(Pattern.compile("\\bwagnis\\b", Pattern.CASE_INSENSITIVE));    }
    @Autowired
    private QuestRepository questRepository;
    private final UserService userService;
    public QuestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<Quest> getAllQuests() {
        List<Quest> quests = questRepository.findAll();
        Collections.shuffle(quests);
        return quests;
    }

    @PostMapping("/suggest")
    public ResponseEntity<Quest> suggestQuest(@RequestBody Quest quest) {
        if (quest.getDescription().toLowerCase().contains("penis")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        }
        Quest newQuest = questRepository.save(quest);
        return ResponseEntity.status(HttpStatus.CREATED).body(newQuest);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeQuest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String uuid = body.get("uuid");
        if (uuid == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            UUID userUuid = UUID.fromString(uuid);
            User user = userService.getUserByUuid(userUuid);
            if (user == null) {
                return ResponseEntity.badRequest().body(null);
            }
            if (user.getLikedQuests().contains(id)) {
                return ResponseEntity.badRequest().body(null);
            } else {
                user.getLikedQuests().add(id);
                questRepository.findById(id).ifPresent(quest -> {
                    quest.setLikes(quest.getLikes() + 1);
                    questRepository.save(quest);
                });
                return ResponseEntity.ok().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Void> dislikeQuest(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String uuid = body.get("uuid");
        if (uuid == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            UUID userUuid = UUID.fromString(uuid);
            User user = userService.getUserByUuid(userUuid);
            if (user == null) {
                return ResponseEntity.badRequest().body(null);
            }
            if (user.getDislikedQuests().contains(id)) {
                return ResponseEntity.badRequest().body(null);
            } else {
                user.getDislikedQuests().add(id);
                questRepository.findById(id).ifPresent(quest -> {
                    quest.setDislikes(quest.getDislikes() + 1);
                    questRepository.save(quest);
                });
                return ResponseEntity.ok().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
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

