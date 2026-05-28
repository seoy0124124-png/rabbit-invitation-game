package com.fairytale.mystery.model;

public enum GamePhase {
    WAITING("대기실", "플레이어들이 눈 내리는 저택으로 모이고 있습니다."),
    PROLOGUE("프롤로그", "토끼의 회중시계와 붉은 설산의 저택이 모습을 드러냅니다."),
    PRIVATE_STORY("개인 이야기 공개", "관리자가 공개하면 각자의 비밀 이야기를 확인할 수 있습니다."),
    FIRST_DISCUSSION("1차 토론", "첫 의심을 나누고 서로의 알리바이를 살핍니다."),
    FIRST_HINT("1차 힌트 공개", "의미가 분명하지 않은 감각과 기억이 각자에게 도착합니다."),
    SECOND_DISCUSSION("2차 토론", "각자의 사소한 기억을 꺼내 서로의 말과 맞춰봅니다."),
    SECOND_HINT("2차 힌트 공개", "초반의 이상한 감각이 누군가의 말과 연결되기 시작합니다."),
    THIRD_HINT("3차 힌트 공개", "마지막 단서가 각자의 손에 도착합니다."),
    FINAL_DISCUSSION("최종 토론", "마지막 의심과 추리를 정리합니다."),
    VOTE("투표", "범인이라고 생각하는 인물을 선택합니다."),
    ENDING("엔딩", "진실과 범인의 동기가 공개됩니다.");

    private final String label;
    private final String description;

    GamePhase(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
