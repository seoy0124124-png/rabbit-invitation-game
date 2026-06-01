package com.fairytale.mystery.controller;

import com.fairytale.mystery.model.GameState;
import com.fairytale.mystery.model.Player;
import com.fairytale.mystery.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class GameApiController {
    private final GameService gameService;

    public GameApiController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/api/state")
    public Map<String, Object> state(HttpSession session) {
        GameState state = gameService.state();
        Player player = gameService.currentPlayer(session).orElse(null);
        List<String> visibleHintIds = player == null ? List.of() : gameService.visibleHintIdsFor(player);
        boolean storyRevealed = player != null && gameService.isPersonalStoryRevealedFor(player);
        boolean endingRevealed = gameService.isEndingRevealed();
        GameService.EndingText endingText = endingRevealed
                ? gameService.endingText()
                : new GameService.EndingText("", "");
        return Map.ofEntries(
                Map.entry("phase", state.getPhase().name()),
                Map.entry("phaseLabel", state.getPhase().getLabel()),
                Map.entry("phaseDescription", state.getPhase().getDescription()),
                Map.entry("timerSeconds", state.getTimerSeconds()),
                Map.entry("timerRunning", state.isTimerRunning()),
                Map.entry("personalStoryRevealed", storyRevealed),
                Map.entry("visibleHintIds", visibleHintIds),
                Map.entry("endingRevealed", endingRevealed),
                Map.entry("endingTitle", endingText.title()),
                Map.entry("endingBody", endingText.body())
        );
    }

    @GetMapping("/api/story")
    public ResponseEntity<Map<String, Object>> story(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("revealed", false));
        }
        if (!gameService.isPersonalStoryRevealedFor(player)) {
            return ResponseEntity.ok(Map.of("revealed", false));
        }
        String[] secretParts = splitSecretAndActingHint(player.secret());
        return ResponseEntity.ok(Map.of(
                "revealed", true,
                "personalStory", player.personalStory(),
                "secret", secretParts[0],
                "actingHint", secretParts[1]
        ));
    }

    @GetMapping("/api/hints")
    public ResponseEntity<List<Map<String, Object>>> hints(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(gameService.stageHintsFor(player).stream()
                .map(hint -> {
                    boolean readable = gameService.canReadHint(player, hint);
                    boolean self = hint.playerCode().equals(player.code());
                    Player owner = gameService.playerByCode(hint.playerCode()).orElse(null);
                    return Map.<String, Object>ofEntries(
                            Map.entry("id", hint.id()),
                            Map.entry("playerCode", hint.playerCode()),
                            Map.entry("playerName", owner == null ? hint.playerCode() : owner.name()),
                            Map.entry("self", self),
                            Map.entry("readable", readable),
                            Map.entry("type", hint.type().getLabel()),
                            Map.entry("typeCode", hint.type().name()),
                            Map.entry("roundLabel", hint.roundLabel()),
                            Map.entry("title", hint.title()),
                            Map.entry("body", readable ? hint.body() : ""),
                            Map.entry("shared", gameService.isHintShared(hint.id()))
                    );
                })
                .toList());
    }

    @GetMapping("/api/evidence/public")
    public ResponseEntity<List<Map<String, Object>>> publicEvidence(HttpSession session) {
        if (gameService.currentPlayer(session).isEmpty()) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(gameService.publicEvidenceBoard().stream()
                .map(this::simpleEvidenceMap)
                .toList());
    }

    @GetMapping("/api/evidence/private")
    public ResponseEntity<List<Map<String, Object>>> privateEvidence(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(gameService.privateEvidenceForPlayer(player).stream()
                .map(evidence -> privateEvidenceMap(evidence, gameService.isPrivateEvidencePublished(evidence.id())))
                .toList());
    }

    @GetMapping("/api/evidence-board")
    public ResponseEntity<Map<String, Object>> evidenceBoard(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "publicEvidence", gameService.publicEvidenceBoard().stream().map(this::simpleEvidenceMap).toList(),
                "privateEvidence", gameService.privateEvidenceForPlayer(player).stream()
                        .map(evidence -> privateEvidenceMap(evidence, gameService.isPrivateEvidencePublished(evidence.id())))
                        .toList(),
                "publishedPrivateEvidence", gameService.publishedPrivateEvidenceBoard().stream()
                        .map(this::publishedPrivateEvidenceMap)
                        .toList()
        ));
    }

    @PostMapping("/api/evidence/private/publish")
    public ResponseEntity<Map<String, Object>> publishPrivateEvidence(@RequestParam String evidenceId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        GameService.PublishEvidenceResult result = gameService.publishPrivateEvidence(player, evidenceId);
        Map<String, Object> body = Map.of("success", result.success(), "message", result.message());
        if (result.forbidden()) {
            return ResponseEntity.status(403).body(body);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/api/public-statuses")
    public ResponseEntity<List<Map<String, Object>>> publicStatuses(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(gameService.publicInfoStatusesFor(player).stream()
                .map(status -> Map.<String, Object>of(
                        "code", status.player().code(),
                        "name", status.player().name(),
                        "self", status.self(),
                        "hintsShared", status.hintsShared()
                ))
                .toList());
    }

    @GetMapping("/api/public-records")
    public ResponseEntity<List<Map<String, Object>>> publicRecords(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(gameService.publicRecords().stream()
                .map(record -> Map.<String, Object>of(
                        "id", record.id(),
                        "playerCode", record.playerCode(),
                        "playerName", record.playerName(),
                        "type", record.type(),
                        "title", record.title(),
                        "body", record.body(),
                        "phaseLabel", record.phaseLabel()
                ))
                .toList());
    }

    @PostMapping("/api/vote")
    public ResponseEntity<Map<String, Object>> vote(@RequestParam String suspectCode, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        GameService.VoteResult result = gameService.vote(player, suspectCode);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
    }

    @PostMapping("/api/share/hint")
    public ResponseEntity<Map<String, Object>> shareHint(@RequestParam String hintId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        GameService.ShareResult result = gameService.shareMyHint(player, hintId);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
    }

    private Map<String, Object> simpleEvidenceMap(GameService.SimpleEvidence evidence) {
        return Map.of(
                "id", evidence.id(),
                "type", evidence.type().name(),
                "title", evidence.title(),
                "locationText", evidence.locationText(),
                "publicDescription", evidence.publicDescription(),
                "description", evidence.publicDescription()
        );
    }

    private Map<String, Object> privateEvidenceMap(GameService.SimpleEvidence evidence, boolean published) {
        return Map.of(
                "id", evidence.id(),
                "type", evidence.type().name(),
                "title", evidence.title(),
                "locationText", evidence.locationText(),
                "publicDescription", evidence.publicDescription(),
                "description", evidence.publicDescription(),
                "privateInterpretation", evidence.privateInterpretation(),
                "status", published ? "PUBLIC" : "PRIVATE",
                "publishable", !published
        );
    }

    private Map<String, Object> publishedPrivateEvidenceMap(GameService.PublishedPrivateEvidenceView evidence) {
        return Map.of(
                "id", evidence.id(),
                "type", "PUBLISHED_PRIVATE",
                "title", evidence.title(),
                "locationText", evidence.locationText(),
                "publicDescription", evidence.publicDescription(),
                "description", evidence.publicDescription(),
                "publishedByCharacterCode", evidence.publishedByCharacterCode(),
                "publishedByName", evidence.publishedByName(),
                "publishedAt", evidence.publishedAt().toString()
        );
    }

    private String[] splitSecretAndActingHint(String source) {
        String text = source == null ? "" : source;
        int actingIndex = text.indexOf("연기 힌트");
        if (actingIndex < 0) {
            return new String[] {text, ""};
        }
        return new String[] {
                text.substring(0, actingIndex).trim(),
                text.substring(actingIndex).trim()
        };
    }
}
