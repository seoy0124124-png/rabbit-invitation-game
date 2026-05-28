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

import java.util.Map;
import java.util.List;

@RestController
public class GameApiController {
    private final GameService gameService;

    public GameApiController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/api/state")
    public Map<String, Object> state(HttpSession session) {
        GameState state = gameService.state();
        List<String> visibleHintIds = gameService.currentPlayer(session)
                .map(gameService::visibleHintIdsFor)
                .orElse(List.of());
        boolean storyRevealed = gameService.currentPlayer(session)
                .map(gameService::isPersonalStoryRevealedFor)
                .orElse(false);
        boolean endingRevealed = gameService.isEndingRevealed();
        GameService.EndingText endingText = endingRevealed
                ? gameService.endingText()
                : new GameService.EndingText("", "");
        return Map.of(
                "phase", state.getPhase().name(),
                "phaseLabel", state.getPhase().getLabel(),
                "phaseDescription", state.getPhase().getDescription(),
                "timerSeconds", state.getTimerSeconds(),
                "timerRunning", state.isTimerRunning(),
                "personalStoryRevealed", storyRevealed,
                "visibleHintIds", visibleHintIds,
                "endingRevealed", endingRevealed,
                "endingTitle", endingText.title(),
                "endingBody", endingText.body()
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

    @GetMapping("/api/hints")
    public ResponseEntity<List<Map<String, Object>>> hints(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        List<Map<String, Object>> hints = gameService.visibleHintsFor(player).stream()
                .map(hint -> Map.<String, Object>of(
                        "id", hint.id(),
                        "type", hint.type().getLabel(),
                        "typeCode", hint.type().name(),
                        "roundLabel", hint.roundLabel(),
                        "title", hint.title(),
                        "body", hint.body(),
                        "shared", gameService.isHintShared(hint.id())
                ))
                .toList();
        return ResponseEntity.ok(hints);
    }

    @GetMapping("/api/public-statuses")
    public ResponseEntity<List<Map<String, Object>>> publicStatuses(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        List<Map<String, Object>> statuses = gameService.publicInfoStatusesFor(player).stream()
                .map(status -> Map.<String, Object>of(
                        "code", status.player().code(),
                        "name", status.player().name(),
                        "self", status.self(),
                        "hintsShared", status.hintsShared()
                ))
                .toList();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/api/public-records")
    public ResponseEntity<List<Map<String, Object>>> publicRecords(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        List<Map<String, Object>> records = gameService.publicRecords().stream()
                .map(record -> Map.<String, Object>of(
                        "id", record.id(),
                        "playerCode", record.playerCode(),
                        "playerName", record.playerName(),
                        "type", record.type(),
                        "title", record.title(),
                        "body", record.body(),
                        "phaseLabel", record.phaseLabel()
                ))
                .toList();
        return ResponseEntity.ok(records);
    }

    @GetMapping("/api/evidence")
    public ResponseEntity<List<Map<String, Object>>> evidence(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        List<Map<String, Object>> evidence = gameService.sharedEvidenceHints().stream()
                .map(item -> Map.<String, Object>of(
                        "id", item.id(),
                        "revealRound", item.roundLabel(),
                        "category", item.type().getLabel(),
                        "name", item.title(),
                        "shortDescription", item.body().split("\\R", 2)[0],
                        "detail", item.body(),
                        "keywords", List.of(item.type().getLabel(), item.roundLabel()),
                        "visual", "paper"
                ))
                .toList();
        return ResponseEntity.ok(evidence);
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
}
