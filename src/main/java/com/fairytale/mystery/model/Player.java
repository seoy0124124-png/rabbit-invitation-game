package com.fairytale.mystery.model;

public record Player(
        String code,
        String name,
        String role,
        String publicDescription,
        String personalStory,
        String secret,
        String color
) {
}
