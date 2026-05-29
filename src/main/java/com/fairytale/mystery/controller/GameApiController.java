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
                Map.entry("investigationOpen", state.isInvestigationOpen()),
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

    @GetMapping("/api/evidence")
    public ResponseEntity<List<Map<String, Object>>> evidence(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(List.of());
        }
        return ResponseEntity.ok(gameService.sharedEvidenceHints().stream()
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
                .toList());
    }

    @GetMapping("/api/investigation")
    public ResponseEntity<Map<String, Object>> investigation(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "locations", gameService.investigationLocationStatusesFor(player).stream().map(this::locationMap).toList(),
                "privateEvidence", gameService.privateEvidenceFor(player).stream().map(this::playerEvidenceMap).toList(),
                "publicEvidence", gameService.publicInvestigationEvidenceFor(player).stream().map(this::playerEvidenceMap).toList()
        ));
    }

    @PostMapping("/api/investigation/location/enter")
    public ResponseEntity<Map<String, Object>> enterInvestigationLocation(@RequestParam String locationId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        GameService.InvestigationActionResult result = gameService.enterInvestigationLocation(player, locationId);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
    }

    @PostMapping("/api/investigation/location/leave")
    public ResponseEntity<Map<String, Object>> leaveInvestigationLocation(@RequestParam String locationId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        GameService.InvestigationActionResult result = gameService.leaveInvestigationLocation(player, locationId);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
    }

    @PostMapping("/api/investigation/evidence/discover")
    public ResponseEntity<Map<String, Object>> discoverInvestigationEvidence(@RequestParam String evidenceId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        return ResponseEntity.ok(evidenceDetailResponse(gameService.discoverInvestigationEvidence(player, evidenceId)));
    }

    @GetMapping("/api/investigation/evidence/detail")
    public ResponseEntity<Map<String, Object>> investigationEvidenceDetail(@RequestParam String evidenceId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        return ResponseEntity.ok(evidenceDetailResponse(gameService.evidenceDetail(player, evidenceId)));
    }

    @PostMapping("/api/investigation/evidence/publish")
    public ResponseEntity<Map<String, Object>> publishInvestigationEvidence(@RequestParam String evidenceId, HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "입장이 필요합니다."));
        }
        GameService.InvestigationActionResult result = gameService.publishInvestigationEvidence(player, evidenceId);
        return ResponseEntity.ok(Map.of("success", result.success(), "message", result.message()));
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

    private Map<String, Object> evidenceDetailResponse(GameService.EvidenceDetailResult result) {
        if (!result.success() || result.evidence() == null) {
            return Map.of("success", result.success(), "message", result.message());
        }
        return Map.of(
                "success", true,
                "message", result.message(),
                "evidence", evidenceDetailMap(result.evidence())
        );
    }

    private Map<String, Object> evidenceDetailMap(GameService.EvidenceDetailView evidence) {
        return Map.of(
                "id", evidence.id(),
                "title", evidence.title(),
                "location", evidence.locationName() + " - " + evidence.detailLocation(),
                "commonDescription", evidence.commonDescription(),
                "personalInterpretation", evidence.personalInterpretation(),
                "recalledLine", evidence.recalledLine(),
                "publicItem", evidence.publicItem(),
                "publishedByName", evidence.publishedByName()
        );
    }

    private Map<String, Object> locationMap(GameService.InvestigationLocationStatus location) {
        return Map.of(
                "id", location.id(),
                "act", location.act(),
                "name", location.name(),
                "description", location.description(),
                "open", location.open(),
                "occupied", location.occupied(),
                "occupiedBySelf", location.occupiedBySelf(),
                "canEnter", location.canEnter(),
                "occupiedByName", location.occupiedByName(),
                "evidenceItems", location.evidenceItems().stream().map(evidence -> Map.of(
                        "id", evidence.id(),
                        "title", evidence.title(),
                        "detailLocation", evidence.detailLocation(),
                        "publicItem", evidence.publicItem(),
                        "privateFound", evidence.privateFound()
                )).toList()
        );
    }

    private Map<String, Object> playerEvidenceMap(GameService.PlayerEvidenceView evidence) {
        return Map.of(
                "id", evidence.id(),
                "title", evidence.title(),
                "locationName", evidence.locationName(),
                "detailLocation", evidence.detailLocation(),
                "locationId", evidence.locationId(),
                "ownerName", evidence.ownerName(),
                "status", evidence.status().name()
        );
    }
}
