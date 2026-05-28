# 붉은 설산의 저택

Spring Boot + Thymeleaf 기반의 정통 머더미스터리 웹게임 초안입니다.

## 현재 포함된 흐름

- 입장코드 로그인: `RED`, `ALICE`, `PINO`, `MATCH`, `ADMIN`
- 플레이어별 프롤로그 1회 표시
- 플레이어 메인: 연극 무대형 저택 화면, 캐릭터 그림자, 개인 힌트/개인 이야기 버튼
- 관리자 화면: 이전/다음 단계, 타이머 설정, 시작, 정지, 초기화
- 관리자 공개 제어: 개인 이야기, 1차 힌트, 2차 힌트를 전체 수신 가능 상태로 공개
- 플레이어 공유: 관리자가 공개한 뒤 자기 개인 이야기/힌트를 전체 공개로 전환 가능
- 관리자 확인: 입장 플레이어 목록, 투표 현황
- 지도 이미지는 분위기 연출용으로만 사용
- 장소 이동, 조사 횟수, 방탈출형 퍼즐 없이 토론과 정보 연결에 집중
- 정보 비대칭 중심: 각 플레이어는 자신에게 공개된 힌트만 확인
- 힌트 구조: 초반에는 의미가 애매한 감각/기억, 후반에는 기존 힌트를 재해석하게 만드는 연결 정보
- 투표 시스템: 투표 단계에서 범인 지목

## 실행

이 폴더에는 Maven Wrapper가 포함되어 있어 Maven을 따로 설치하지 않아도 실행할 수 있습니다.

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux에서는 다음처럼 실행합니다.

```bash
./mvnw spring-boot:run
```

실행 후 브라우저에서 `http://localhost:8080`으로 접속합니다.

처음 실행할 때는 Apache Maven 배포본을 `.mvn/wrapper` 아래로 내려받기 때문에 인터넷 연결이 필요합니다. 이후에는 내려받은 Maven으로 실행됩니다. Java 17은 필요합니다.

## 주요 파일

- 서버 상태 관리: `src/main/java/com/fairytale/mystery/service/GameService.java`
- 입장/프롤로그/로비/지도: `src/main/java/com/fairytale/mystery/controller/HomeController.java`
- 관리자 진행 제어: `src/main/java/com/fairytale/mystery/controller/AdminController.java`
- 화면 동기화 API: `src/main/java/com/fairytale/mystery/controller/GameApiController.java`
- 화면 템플릿: `src/main/resources/templates`
- 분위기 스타일: `src/main/resources/static/css/game.css`
- 상호작용 스크립트: `src/main/resources/static/js/game.js`
- 저택 지도 자산: `src/main/resources/static/images/mansion-map.svg`
