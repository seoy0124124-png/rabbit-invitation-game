package com.fairytale.mystery.controller;

import com.fairytale.mystery.model.Player;
import com.fairytale.mystery.service.GameService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    private final GameService gameService;

    public HomeController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        return gameService.currentPlayer(session)
                .map(player -> player.code().equals("ADMIN") ? "redirect:/admin" : "redirect:/prologue")
                .orElse("index");
    }

    @PostMapping("/enter")
    public String enter(@RequestParam @NotBlank String code, HttpSession session, Model model) {
        return gameService.login(code, session)
                .map(player -> player.code().equals("ADMIN") ? "redirect:/admin" : "redirect:/prologue")
                .orElseGet(() -> {
                    model.addAttribute("error", "초대장에 적힌 입장코드를 다시 확인하세요.");
                    return "index";
                });
    }

    @PostMapping("/leave")
    public String leave(HttpSession session) {
        gameService.leave(session);
        return "redirect:/";
    }

    @GetMapping("/prologue")
    public String prologue(HttpSession session, Model model) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return "redirect:/";
        }
        if (gameService.isPrologueDone(session)) {
            return "redirect:/lobby";
        }
        model.addAttribute("player", player);
        model.addAttribute("prologue", gameService.prologueFor(player));
        return "prologue";
    }

    @PostMapping("/prologue/finish")
    public String finishPrologue(HttpSession session) {
        gameService.finishPrologue(session);
        return "redirect:/lobby";
    }

    @GetMapping("/lobby")
    public String lobby(HttpSession session, Model model) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return "redirect:/";
        }
        model.addAttribute("player", player);
        model.addAttribute("state", gameService.state());
        model.addAttribute("hints", gameService.visibleHintsFor(player));
        model.addAttribute("suspects", gameService.playableCharacters());
        model.addAttribute("storyRevealed", gameService.isPersonalStoryRevealedFor(player));
        model.addAttribute("publicStatuses", gameService.publicInfoStatusesFor(player));
        model.addAttribute("publicRecords", gameService.publicRecords());
        return "lobby";
    }

    @GetMapping("/map")
    public String map(HttpSession session, Model model) {
        Player player = gameService.currentPlayer(session).orElse(null);
        if (player == null) {
            return "redirect:/";
        }
        model.addAttribute("player", player);
        model.addAttribute("state", gameService.state());
        return "map";
    }
}
