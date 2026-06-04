package com.fairytale.mystery.controller;

import com.fairytale.mystery.model.GamePhase;
import com.fairytale.mystery.model.Player;
import com.fairytale.mystery.service.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {
    private final GameService gameService;

    public AdminController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }
        model.addAttribute("state", gameService.state());
        model.addAttribute("phases", gameService.phases());
        model.addAttribute("players", gameService.playerStatuses());
        model.addAttribute("votes", gameService.voteStatuses());
        model.addAttribute("disclosures", gameService.playerDisclosureStatuses());
        model.addAttribute("globalDisclosure", gameService.globalDisclosureStatus());
        model.addAttribute("publicRecords", gameService.publicRecords());
        model.addAttribute("endingRevealed", gameService.isEndingRevealed());
        model.addAttribute("evidenceReleaseStatus", gameService.evidenceReleaseStatus());
        model.addAttribute("privateEvidenceStatuses", gameService.privateEvidenceAdminStatuses());
        model.addAttribute("progressSteps", gameService.adminProgressSteps());
        return "admin";
    }

    @PostMapping("/admin/phase/next")
    public String nextPhase(HttpSession session) {
        if (isAdmin(session)) {
            gameService.nextPhase();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/phase/previous")
    public String previousPhase(HttpSession session) {
        if (isAdmin(session)) {
            gameService.previousPhase();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/timer/set")
    public String setTimer(@RequestParam(value = "minutes", defaultValue = "10") int minutes,
                           @RequestParam(value = "seconds", defaultValue = "0") int seconds,
                           HttpSession session) {
        if (isAdmin(session)) {
            gameService.setTimer(minutes, seconds);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/timer/start")
    public String startTimer(HttpSession session) {
        if (isAdmin(session)) {
            gameService.startTimer();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/timer/stop")
    public String stopTimer(HttpSession session) {
        if (isAdmin(session)) {
            gameService.stopTimer();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/timer/reset")
    public String resetTimer(HttpSession session) {
        if (isAdmin(session)) {
            gameService.resetTimer();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/story")
    public String revealStory(HttpSession session) {
        if (isAdmin(session) && gameService.canRunProgressStep(1)) {
            gameService.unlockPersonalStories();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/player-story")
    public String revealPlayerStory(@RequestParam("playerCode") String playerCode, HttpSession session) {
        if (isAdmin(session)) {
            gameService.unlockPersonalStory(playerCode);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/hint")
    public String revealHint(@RequestParam("hintId") String hintId, HttpSession session) {
        if (isAdmin(session)) {
            gameService.revealHint(hintId);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/release/hints")
    public String releaseHints(@RequestParam("phase") GamePhase phase, HttpSession session) {
        if (isAdmin(session)) {
            gameService.releaseHintRound(phase);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/evidence/public/release")
    public String releasePublicEvidence(@RequestParam("order") int order, HttpSession session) {
        if (isAdmin(session)) {
            gameService.releasePublicEvidence(order);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/evidence/private/release")
    public String releasePrivateEvidence(@RequestParam("order") int order, HttpSession session) {
        if (isAdmin(session)) {
            gameService.releasePrivateEvidence(order);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/discussion/start")
    public String startDiscussion(@RequestParam("phase") GamePhase phase,
                                  @RequestParam(value = "minutes", defaultValue = "10") int minutes,
                                  @RequestParam(value = "seconds", defaultValue = "0") int seconds,
                                  HttpSession session) {
        int step = phase == GamePhase.FIRST_DISCUSSION ? 2 : 3;
        if (isAdmin(session) && gameService.canRunProgressStep(step)) {
            gameService.startDiscussion(phase, minutes, seconds);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/discussion/end")
    public String endDiscussion(@RequestParam("phase") GamePhase phase, HttpSession session) {
        boolean valid = phase == GamePhase.FIRST_DISCUSSION || phase == GamePhase.SECOND_DISCUSSION;
        if (isAdmin(session) && valid) {
            gameService.finishDiscussion(phase);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/vote/start")
    public String startVote(HttpSession session) {
        if (isAdmin(session) && gameService.canRunProgressStep(4)) {
            gameService.startVote();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/vote/end")
    public String endVote(HttpSession session) {
        if (isAdmin(session) && gameService.canRunProgressStep(5)) {
            gameService.closeVote();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/ending")
    public String revealEnding(HttpSession session) {
        if (isAdmin(session) && gameService.canRunProgressStep(6)) {
            gameService.revealEnding();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reset")
    public String resetGame(@RequestParam("resetCode") String resetCode, HttpSession session) {
        if (isAdmin(session) && "RESET".equals(resetCode)) {
            gameService.resetGameState();
        }
        return "redirect:/admin";
    }

    private boolean isAdmin(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        return player != null && player.code().equals("ADMIN");
    }
}
