package com.fairytale.mystery.model;

public record PersonalHint(
        String id,
        String playerCode,
        HintType type,
        String roundLabel,
        GamePhase phase,
        String title,
        String body
) {
    public enum HintType {
        TESTIMONY("증언"),
        EVIDENCE("물증");

        private final String label;

        HintType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
