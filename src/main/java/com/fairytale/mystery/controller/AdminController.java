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
        model.addAttribute("hintRounds", gameService.hintRoundStatuses());
        model.addAttribute("endingRevealed", gameService.isEndingRevealed());
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
    public String setTimer(@RequestParam(defaultValue = "10") int minutes,
                           @RequestParam(defaultValue = "0") int seconds,
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
        if (isAdmin(session)) {
            gameService.unlockPersonalStories();
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/hint")
    public String revealHint(@RequestParam String hintId, HttpSession session) {
        if (isAdmin(session)) {
            gameService.revealHint(hintId);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/release/hints")
    public String releaseHints(@RequestParam GamePhase phase, HttpSession session) {
        if (isAdmin(session)) {
            gameService.releaseHintRound(phase);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/evidence")
    public String revealEvidence(@RequestParam String evidenceId, HttpSession session) {
        if (isAdmin(session)) {
            gameService.revealEvidence(evidenceId);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reveal/ending")
    public String revealEnding(HttpSession session) {
        if (isAdmin(session)) {
            gameService.revealEnding();
        }
        return "redirect:/admin";
    }

    private boolean isAdmin(HttpSession session) {
        Player player = gameService.currentPlayer(session).orElse(null);
        return player != null && player.code().equals("ADMIN");
    }
}
