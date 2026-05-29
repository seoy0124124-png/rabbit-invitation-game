package com.fairytale.mystery.service;

import com.fairytale.mystery.model.GamePhase;
import com.fairytale.mystery.model.GameState;
import com.fairytale.mystery.model.PersonalHint;
import com.fairytale.mystery.model.PersonalHint.HintType;
import com.fairytale.mystery.model.Player;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GameService {
    private static final String PLAYER_CODE = "playerCode";
    private static final String PROLOGUE_DONE = "prologueDone";

    private final GameState state = new GameState();
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final Map<String, PersonalHint> hints = new LinkedHashMap<>();
    private final Map<String, EvidenceItem> evidenceItems = new LinkedHashMap<>();
    private final Map<String, StoryContent> storyContents = new LinkedHashMap<>();
    private final Set<String> revealedHintIds = new LinkedHashSet<>();
    private final Set<String> revealedEvidenceIds = new LinkedHashSet<>();
    private final Set<String> sharedHintIds = new LinkedHashSet<>();
    private final Set<String> unlockedPersonalStoryCodes = new LinkedHashSet<>();
    private final Set<GamePhase> releasedHintPhases = new LinkedHashSet<>();
    private final Set<Integer> openedInvestigationActs = new LinkedHashSet<>();
    private final Map<String, LocationOccupancy> investigationOccupancies = new LinkedHashMap<>();
    private final Map<String, PlayerEvidenceState> playerEvidenceStates = new LinkedHashMap<>();
    private final Map<String, PublishedEvidence> publishedEvidence = new LinkedHashMap<>();
    private final List<PublicRecord> publicRecords = new java.util.ArrayList<>();
    private final Map<String, Instant> enteredPlayers = new LinkedHashMap<>();
    private final Map<String, String> votes = new LinkedHashMap<>();
    private boolean endingRevealed;

    public GameService() {
        players.put("RED", new Player("RED", "빨간망토", "숲길의 목격자", "늑대에게 쫓긴 뒤 사람을 쉽게 믿지 못하게 된 인물",
                """
                        눈은 소리 없이 내렸다.

                        숲은 하얗게 덮여 있었지만, 나는 그 하얀색이 무서웠다. 발자국이 너무 선명하게 남았기 때문이다. 내가 어디서 왔는지, 어디로 도망쳤는지, 누가 나를 따라오고 있는지까지 전부 드러내는 색.

                        늑대의 숨소리는 생각보다 가까웠다.

                        젖은 털 냄새, 흙을 긁는 발톱 소리, 내 목 안에서 찢어지던 숨. 나는 달렸고, 넘어졌고, 다시 일어났다. 바구니는 어디선가 사라졌다. 망토 끝은 가지에 찢겼고, 눈 위에는 붉은 실밥이 흩어졌다.

                        그리고 늑대가 나를 물었다.

                        목덜미에 박힌 이빨의 감각은 아직도 남아 있다. 죽는다고 생각했다. 이상하게도 그 순간, 나는 늑대의 눈을 보았다. 굶주린 눈이 아니었다. 화난 눈도 아니었다. 마치 늦었다고, 더 빨리 도망쳐야 했다고 말하는 눈이었다.

                        그 뒤의 기억은 없다.

                        정신을 차렸을 때 나는 숲 밖에 있었다. 옆에는 작은 발자국이 이어져 있었다. 토끼의 발자국. 나는 그것을 따라 걸었던 것 같은데, 왜 따라갔는지는 기억나지 않는다. 다만 눈보라 속에서 누군가 나를 자꾸 돌아보았던 것만 기억난다. 따라오라고, 멈추지 말라고.

                        그날 이후 밤이 오면 몸이 낯설어진다.

                        손톱은 조금씩 날카로워졌고, 피 냄새는 멀리서도 느껴졌다. 누군가 손가락을 베기만 해도 머릿속이 하얘졌다. 나는 사람들의 목소리보다 심장 뛰는 소리를 먼저 듣게 되었다.

                        그래서 나는 사람을 믿지 않는다.

                        정확히 말하면, 나 자신을 믿지 않는다.

                        오늘 밤도 기억이 끊겼다. 정신을 차렸을 때, 내 손에는 피가 묻어 있었다. 차가운 피였다. 손바닥에만 말라붙어 있었고, 손목 위로는 이상할 만큼 깨끗했다. 바닥에는 찢어진 붉은 천 조각이 있었다. 내 망토와 같은 색.

                        나는 그 조각을 주워 들지 못했다.

                        토끼가 죽었다고 했다.

                        모두가 서로를 보았지만, 나는 내 손만 보았다. 내가 했을까. 또 기억을 잃은 사이에, 내가 정말 괴물이 되었을까. 내가 살아남은 이유는 누군가의 선의가 아니라, 내가 다른 것을 잡아먹기 위해서였을까.

                        눈 내리는 숲에서 토끼가 나를 구했다면, 나는 오늘 밤 그 토끼를 죽인 셈이 된다.

                        그 생각이 가장 무섭다.
                        """,
                "늑대에게 물린 뒤부터 기억이 끊기는 밤이 생겼다. 당신은 토끼를 죽였는지보다, 자신이 구원받을 가치가 있었는지 두려워한다.", "#a91f2f"));
        players.put("ALICE", new Player("ALICE", "앨리스", "기묘한 손님", "환각처럼 보이는 장면 속에서 실제 단서를 말하는 인물",
                """
                        나는 늘 이상한 아이였다.

                        현실에서는 꿈을 너무 많이 꾼다고 했고, 꿈속에서는 현실 냄새가 난다고 했다. 어디에 있어도 조금씩 어긋나 있었다. 사람들이 웃으면 한 박자 늦게 웃었고, 모두가 조용해지면 벽 너머에서 들리는 발소리를 들었다.

                        오래전 숲에서도 그랬다.

                        눈이 내리고 있었다. 하늘은 낮인데도 저녁처럼 어두웠고, 나뭇가지들은 손가락처럼 길었다. 나는 길을 잃었지만, 사실 길을 잃었다는 말도 이상했다. 처음부터 내가 속한 길 같은 건 없었으니까.

                        그때 소리를 들었다.

                        똑딱.

                        숲속에 시계가 있을 리 없는데, 분명 초침 소리가 났다. 뒤이어 나무가 안쪽에서 갈라지는 듯한 소리, 아주 가느다란 실이 팽팽하게 당겨졌다 끊어지는 소리가 들렸다.

                        그리고 토끼가 나타났다.

                        아니, 토끼였을 것이다. 모두가 그렇게 말했으니까. 하지만 나는 가끔 그를 다르게 보았다. 눈 사이로 흰 귀가 흔들리다가도, 순간적으로 긴 손가락과 젖은 외투 자락이 보였다. 작은 발자국 옆에 사람의 구두 자국 같은 것이 겹쳐 보였다.

                        나는 무섭지 않았다.

                        그는 나를 보며 고개를 기울였다. 마치 내가 이상한 것을 보는 게 아니라, 남들이 보지 못하는 것을 놓치지 않았다고 말해주는 것처럼.

                        "잊지 않으면 길을 잃게 될 거야."

                        그 말은 저주처럼도 들렸고, 부탁처럼도 들렸다.

                        그날 이후 나는 더 자주 이상한 장면을 본다. 멈춘 시계. 벽 속에서 굴러가는 톱니. 웃고 있는 누군가의 입가. 바닥에 떨어진 나무 가루. 손목에서 끊어진 실.

                        사람들은 내 말을 듣고 눈을 피한다. 앨리스가 또 시작했다고 생각한다. 환각이라고, 망상이라고, 너무 많은 이야기를 읽은 탓이라고.

                        하지만 나는 안다.

                        내가 보는 것은 전부 거짓이 아니다. 현실이 나를 밀어냈기 때문에, 나는 현실의 틈을 보게 된 것뿐이다.

                        오늘 밤 저택의 시계는 11시 47분에 멈춰 있었다. 모두가 그 시간을 보고 놀랐지만, 나는 그보다 먼저 들었다. 벽 안쪽에서 작게 돌아가던 기계음. 누군가 시간을 멈춘 것이 아니라, 시간이 멈춘 것처럼 보이게 만든 소리.

                        토끼가 죽었다고 했을 때, 나는 울지 못했다.

                        대신 숲속의 그 목소리가 다시 들렸다.

                        잊지 않으면 길을 잃게 될 거야.

                        그렇다면 나는 지금도 길을 잃은 걸까. 아니면 모두가 잊어버린 길 위에, 나 혼자 아직 서 있는 걸까.
                        """,
                "당신은 환각이라고 불리는 장면 속에서 진실의 가장자리를 본다. 문제는 그것을 믿어줄 사람이 아무도 없다는 것이다.", "#4f86a8"));
        players.put("PINO", new Player("PINO", "피노키오", "거짓말의 증인", "사건이 일어난 밤의 이야기가 계속 달라지는 인물",
                """
                        나는 태어난 것이 아니라 만들어졌다.

                        처음 눈을 떴을 때 맡은 냄새는 젖은 나무와 기름, 오래된 금속의 냄새였다. 천장에는 실이 매달려 있었고, 내 손목에도, 발목에도, 목 뒤에도 가느다란 줄이 이어져 있었다. 누군가 나를 움직였다. 손가락을 굽히고, 고개를 들게 하고, 웃는 법을 가르쳤다.

                        웃어라.

                        울어라.

                        거짓말해라.

                        나는 배웠다. 너무 빨리 배웠다. 사람의 눈이 어디서 흔들리는지, 어떤 기억을 건드리면 목소리가 작아지는지, 어떤 말을 심으면 진실이 스스로 길을 잃는지.

                        내 안에는 처음부터 악의가 있었다. 내 것이 아닌데, 내 속에서 자랐다. 누군가 심어둔 작은 톱니처럼, 사람의 기억이 삐걱거릴 때마다 기뻐하도록 만들어진 마음.

                        그런데 이상한 일이 생겼다.

                        나는 사람들을 오래 보았다. 추위에 떠는 손, 거짓말 뒤에 숨는 울음, 아무도 보지 않을 때 무너지는 얼굴. 처음에는 그 모든 것이 조작하기 쉬운 장치처럼 보였다. 하지만 어느 순간부터 아팠다. 남의 기억을 흐리게 만들면, 내 안쪽도 같이 깎여나갔다.

                        숲에서 토끼를 만난 날, 나는 처음으로 도망쳤다.

                        눈보라가 실처럼 쏟아졌고, 나를 묶고 있던 보이지 않는 줄들이 나뭇가지에 걸려 삐걱거렸다. 나는 계속 넘어졌다. 넘어질 때마다 내 무릎에서는 피 대신 나무 가루가 묻어났다.

                        토끼가 말했다.

                        "선택해."

                        나는 그 말을 몰랐다. 명령도 아니고, 암호도 아니고, 조종도 아닌 말. 그래서 따라갔다. 처음으로 누군가의 손에 끌려가지 않고, 내 발로.

                        오늘 밤 토끼는 나를 알아보았다.

                        아니, 정확히는 내가 무엇으로 만들어졌는지 알아보았다. 그의 눈이 내 손목의 보이지 않는 실을 따라갔다. 벽 속의 기계음이 멎었고, 내 머리 안에서 오래된 목소리가 속삭였다.

                        지워.

                        나는 그러지 않으려고 했다. 정말이다. 믿어달라고 말하고 싶지만, 내 말은 늘 나보다 먼저 도망친다. 나는 기억을 건드릴 수 있다. 그래서 내가 건드리지 않았다고 말할 때조차, 그 말은 의심받아 마땅하다.

                        나는 토끼의 기억을 지우려 했다.

                        그 순간 내 안의 악의가 기뻐했다. 그리고 나는 그 기쁨을 느낀 내 자신이 끔찍했다. 내가 인간이 되고 싶었던 건, 착해지고 싶어서가 아니라 더는 누군가의 도구이고 싶지 않아서였다.

                        지금 나는 어느 장면까지가 진짜인지 모른다. 토끼가 쓰러진 순간, 내 손끝에 남은 나무 가루, 누군가의 기억에서 빠져나온 비명, 11시 47분이라는 숫자.

                        내가 범인에 가장 가까운 존재일지도 모른다.

                        하지만 나는 아직도 묻고 싶다.

                        만들어진 것도 용서받을 수 있을까. 악의에서 태어난 것도, 마지막에는 다른 선택을 할 수 있을까.
                        """,
                "당신은 사람의 기억을 흔들 수 있다. 하지만 오늘 밤 가장 믿을 수 없는 기억은, 당신 자신의 것이다.", "#b9863a"));
        players.put("MATCH", new Player("MATCH", "성냥팔이 소녀", "불씨를 가진 아이", "중요한 이야기가 나오면 농담으로 화제를 돌리는 인물",
                """
                        나는 불을 좋아했다.

                        사람들은 그 말을 들으면 겁먹은 얼굴을 한다. 그래서 나는 늘 장난처럼 말했다. 추우니까 그렇지. 손이 얼면 성냥 하나쯤 그을 수도 있잖아. 웃으면서 말하면 사람들은 더 캐묻지 않았다.

                        하지만 사실은 달랐다.

                        불은 나를 미워하지 않았다. 사람들은 내 옷이 더럽다고 밀어냈고, 내 손이 차갑다고 피했고, 내 목소리가 크다고 꾸짖었다. 하지만 불은 내가 가까이 가면 흔들렸다. 작게 타오르며 대답했다. 세상에서 유일하게 내 곁에 머물러주는 것 같았다.

                        처음에는 작은 장난이었다. 젖은 종이, 버려진 리본, 아무도 찾지 않는 상자. 타는 냄새가 나면 마음이 조용해졌다. 불길이 커질수록 내 안의 소란도 잠잠해졌다.

                        그러다 어느 날, 숲이 탔다.

                        내가 그런 게 아니라고 말하고 싶었다. 적어도 전부는 아니었다고. 하지만 성냥을 그은 건 나였다. 붉은 빛이 눈 덮인 가지 위로 번지고, 젖은 연기가 목구멍을 긁었다. 하늘은 저녁보다 더 붉었고, 나무들은 울음소리처럼 갈라졌다.

                        나는 도망치지 못했다.

                        그때 토끼가 나타났다.

                        작은 몸으로는 말이 되지 않을 만큼 침착했다. 불길 앞에서 그는 나를 보았다. 비난하지 않았다. 왜 그랬냐고 묻지도 않았다. 다만 말했다.

                        "따라와."

                        나는 그 말을 붙잡았다. 누군가 처음으로 내 손목이 아니라 내 이름을 부른 것 같았다. 불길 속에서, 연기 속에서, 나는 토끼를 따라 숲 밖으로 나왔다.

                        그 뒤로 나는 더 많이 웃었다.

                        웃으면 죄책감이 덜 들었다. 농담을 던지면 사람들은 내 눈을 오래 보지 않았다. 중요한 이야기가 나오면 나는 일부러 컵을 엎거나, 쓸데없는 말을 하거나, 누군가의 표정을 흉내 냈다. 모두가 웃는 동안 나는 숨을 골랐다.

                        오늘 밤 사건 직후, 나는 종이를 태웠다.

                        손이 떨렸다. 성냥 끝의 불꽃은 너무 작았는데, 나는 다시 숲 전체가 타는 냄새를 맡았다. 젖은 연기, 기름, 마른 나무. 종이에는 설계도 같은 선들이 있었다. 관절처럼 꺾인 그림, 실이 지나가는 위치, 사람의 것이 아닌 몸을 움직이는 방식.

                        남겨두면 누군가 또 망가질 것 같았다.

                        그래서 태웠다.

                        누가 보면 증거를 없애는 것처럼 보일 것이다. 어쩌면 맞다. 나는 늘 불로 무언가를 지워왔다. 추위도, 외로움도, 내가 저지른 일도.

                        하지만 토끼만은 지우고 싶지 않았다.

                        그가 나를 구했으니까. 내가 가장 나쁜 아이였던 순간에도, 그는 나를 버리지 않았으니까.

                        그래서 더 무섭다.

                        내가 또 불로 누군가를 지우려 한 건 아닌지. 구원받은 사람이, 결국 구원해준 존재의 흔적을 태워버린 건 아닌지.
                        """,
                "당신은 불로 많은 것을 지웠다. 하지만 오늘 밤 태운 것이 증거였는지, 누군가를 지키려는 마지막 몸부림이었는지 확신하지 못한다.", "#d16b42"));
        players.put("ADMIN", new Player("ADMIN", "관리자", "게임 마스터", "저택의 밤을 진행하는 사람",
                "저택의 밤을 진행합니다.", "모든 시계와 문은 당신의 손에서 움직입니다.", "#c9a24a"));

        loadExternalStories();

        addTestimony("red-t1", "RED", "내 손의 빈칸",
                "당신은 사건 직전의 몇 분을 기억하지 못합니다. 목덜미의 오래된 상처가 뜨거워졌고, 눈 냄새와 젖은 털 냄새가 뒤섞였습니다. 누군가를 해쳤다는 장면은 없지만, 그 빈칸 자체가 당신을 겁먹게 합니다.");
        addTestimony("red-t2", "RED", "젖은 털 냄새",
                "11시 47분이라는 말이 나오자 목덜미의 오래된 상처가 뜨거워졌습니다. 늑대의 숨, 눈 냄새, 토끼의 작은 발자국이 한꺼번에 떠올랐지만 순서가 맞지 않습니다. 이 기억을 말하면 모두가 당신을 괴물로 볼 것 같습니다.");
        addTestimony("red-t3", "RED", "ALICE가 먼저 말한 죽음",
                "당신은 사건 전에 ALICE가 조용히 '토끼는 이미 멈췄어'라고 말하는 것을 들었습니다. 그때는 이상한 농담처럼 넘겼지만, 지금 생각하면 그녀는 시체를 보기 전부터 죽음을 알고 있던 사람처럼 보였습니다.");
        addTestimony("red-t4", "RED", "PINO와 나눈 뒤의 빈칸",
                "PINO가 당신에게 '그 장면을 정말 기억해?'라고 묻자 머릿속에서 눈발이 쏟아졌습니다. 대답하려던 순간, 당신은 무슨 말을 하려 했는지 잊었습니다. 이상하게도 그가 놀란 얼굴을 하고 있었다는 것만은 기억납니다.");
        addTestimony("red-t5", "RED", "떨리던 MATCH의 손",
                "사건 직후 MATCH가 손을 뒤로 숨기는 것을 보았습니다. 손끝에서는 검은 재가 떨어졌고, 그녀는 웃으려 했지만 입술이 제대로 움직이지 않았습니다. 불을 무서워하는 사람의 떨림이 아니라, 불을 너무 잘 아는 사람의 떨림처럼 보였습니다.");

        addTestimony("alice-t1", "ALICE", "RED의 붉은 조각",
                "중앙 홀로 가는 복도에서 RED의 망토와 같은 붉은 천 조각을 보았습니다. 하지만 이상합니다. 천 조각을 본 뒤에야 발소리가 들렸습니다. 누군가 먼저 증거를 놓고, 나중에 사람이 지나가게 만든 것처럼 순서가 어긋나 있었습니다.");
        addTestimony("alice-t2", "ALICE", "이미 멈춰 있던 시계",
                "당신은 중앙 홀에 들어섰을 때 시계가 이미 11시 47분에 멈춰 있는 것을 보았습니다. 뒤늦게 들린 초침 소리는 시계가 아니라 벽 안쪽의 작은 기계음 같았습니다. 이 말을 믿어줄 사람은 많지 않을 것입니다.");
        addTestimony("alice-t3", "ALICE", "도망치라고 말하던 RED",
                "사건 직전 RED가 난간 아래에서 혼잣말을 하고 있었습니다. 당신은 분명 '도망쳐...'라는 말을 들었습니다. 누구에게 한 말인지는 모르겠습니다. 토끼에게 한 경고였을 수도, 자기 자신에게 한 명령이었을 수도 있습니다.");
        addTestimony("alice-t4", "ALICE", "이름을 부르는 목소리",
                "토끼가 죽기 직전, 복도 끝에서 아주 낮은 목소리가 들렸습니다. 누군가의 이름을 부르는 목소리였습니다. 이름은 정확히 들리지 않았지만, 그 목소리에는 공포보다 슬픔이 더 많이 섞여 있었습니다. 그 소리를 말하려는 순간, PINO가 아주 짧게 숨을 멈춘 것처럼 보였습니다.");
        addTestimony("alice-t5", "ALICE", "번지는 얼굴",
                "오래된 그림 속 얼굴 하나가 물에 젖은 것처럼 사라지는 장면을 보았습니다. 그 순간 PINO가 당신을 바라보고 있었습니다. 그가 한 일인지, 그가 두려워하던 일이 벌어진 것인지는 알 수 없습니다.");

        addTestimony("pino-t1", "PINO", "ALICE가 본 시계",
                "ALICE는 시계가 이미 멈춰 있었다고 중얼거렸습니다. 모두가 그녀의 말을 흘렸지만, 당신의 머리 안쪽에서 같은 시각이 톱밥처럼 따끔거렸습니다. 그녀가 틀린 말을 한 것이 아니라, 너무 이른 진실을 본 것일지도 모릅니다.");
        addTestimony("pino-t2", "PINO", "말이 먼저 흔들린다",
                "누가 어디에 있었는지 말하려는 순간 문장이 자꾸 바뀝니다. 당신은 거짓말을 하려는 게 아닌데, 말이 먼저 달아납니다. 이 사실을 말하면 모두가 당신을 의심할 것이고, 숨기면 더 깊은 거짓말이 됩니다.");
        addTestimony("pino-t3", "PINO", "MATCH의 탄 냄새",
                "MATCH가 지나간 뒤 복도에는 젖은 연기 냄새가 남았습니다. 누군가 종이를 태웠고, 그녀는 그 사실을 들키지 않으려 했습니다. 하지만 이상하게도 당신은 그 종이에 무엇이 적혀 있었는지 기억하지 못합니다.");
        addTestimony("pino-t4", "PINO", "RED를 본 시간",
                "당신은 방 안 창문으로 저택 반대편 눈밭에 쓰러진 RED를 보았습니다. 그런데 누군가는 같은 시각 중앙 홀에서 RED의 그림자를 봤다고 말합니다. 둘 중 하나는 거짓말이거나, 누군가 기억의 위치를 바꿔놓았습니다.");
        addTestimony("pino-t5", "PINO", "내가 지우려던 얼굴",
                "토끼가 당신의 손목을 잡고 '너는 네가 만들어진 이유보다 더 큰 존재야'라고 말했습니다. 그는 끝까지 당신을 무서워하지 않았습니다. 그 직후의 기억이 끊겨 있습니다. 당신은 그 말을 지우고 싶었던 걸까요, 아니면 그 말을 믿고 싶어서 붙잡았던 걸까요. 누군가 '구원', '버려졌다', '괴물', '혼자 남았다' 같은 말을 꺼내면 손끝부터 먼저 굳어옵니다.");

        addTestimony("match-t1", "MATCH", "RED의 피 냄새",
                "사건 직후 RED는 피 냄새라는 말에 지나치게 예민하게 굳었습니다. 하지만 그녀의 손에는 피가 있었고 옷자락에는 거의 없었습니다. 정말 덮쳐 물어뜯었다면 냄새보다 먼저 몸 전체가 젖어 있었어야 합니다.");
        addTestimony("match-t2", "MATCH", "PINO의 두 번째 대답",
                "당신은 같은 질문을 두 번 했습니다. 첫 번째 PINO는 '토끼는 나를 보지 않았다'고 했고, 두 번째는 '토끼는 나를 알아봤다'고 했습니다. 둘 다 거짓말처럼 들리지 않았다는 점이 더 불길합니다.");
        addTestimony("match-t3", "MATCH", "ALICE의 예고",
                "토끼의 시체가 발견되기 전, ALICE가 냅킨 구석에 붉은 목의 토끼를 그리고 있었습니다. 당신이 묻자 그녀는 '이미 본 장면이야'라고 답했습니다. 장난이라기엔 손끝이 너무 차가웠습니다.");
        addTestimony("match-t4", "MATCH", "서로 다른 종소리",
                "당신은 분명 종소리를 들었습니다. 하지만 RED는 아무 소리도 못 들었다고 했고, ALICE는 시계가 이미 멈춰 있었다고 했습니다. PINO는 한참 뒤에야 '종은 한 번 울렸다'고 말했습니다. 모두 같은 밤에 있었던 게 맞는지 의심됩니다.");
        addTestimony("match-t5", "MATCH", "PINO의 손끝",
                "사건 직후, 당신은 PINO의 손끝을 보았습니다. 피는 없었습니다. 대신 아주 미세한 나무 가루가 손톱 사이에 묻어 있었습니다. 말하려 했지만 입을 다물었습니다. 당신도 그 직후 정체를 알 수 없는 설계도 같은 종이를 태웠기 때문입니다. 누군가를 괴물로 만들기엔, 당신 역시 너무 많은 것을 숨기고 있었습니다.");
        addEvidenceItem("claw-marks", "초반 의심", "RED 의심", "벽의 발톱 자국",
                "중앙 홀 벽에 깊게 긁힌 흔적.",
                "초반에는 RED의 늑대인간 정체를 의심하게 만듭니다. 하지만 긁힌 깊이가 일정하고 시작점이 어색해, 후반에는 누군가 RED를 범인처럼 보이게 만든 흔적일 수 있습니다.",
                List.of("발톱", "늑대", "조작 가능성"), "claw");
        addEvidenceItem("torn-record", "중반 왜곡", "기억 조작", "찢어진 기록 일부",
                "토끼가 남긴 기록 중 일부가 찢겨 있습니다.",
                "남은 문장에는 \"...기억이 다시 흔들리기 시작했다.\"라고 적혀 있습니다. 단순한 훼손보다, 누군가 기억에 관한 기록만 골라 없앤 듯합니다.",
                List.of("기억", "찢긴 기록", "토끼의 메모"), "paper");
        addEvidenceItem("broken-thread", "후반 복선", "토끼의 정체", "끊어진 실 조각",
                "시계 내부 또는 토끼의 방 근처에서 발견된 얇은 실.",
                "실은 옷감용이라기엔 지나치게 질기고 균일합니다. 인형, 조종, 오래된 공학 장치, 그리고 PINO의 존재를 동시에 떠올리게 합니다.",
                List.of("실", "인형", "조종"), "thread");
        addEvidenceItem("torn-red-cloth", "초반 의심", "RED 의심", "찢어진 붉은 천 조각",
                "피가 묻은 붉은 천 일부.",
                "RED의 망토 조각처럼 보입니다. 다만 피가 튄 흔적은 적고 천 끝이 잡아당겨 찢긴 모양이라, 직접 살해 흔적과는 미묘하게 맞지 않습니다.",
                List.of("붉은 천", "피", "망토"), "cloth");
        addEvidenceItem("blurred-ink", "중반 왜곡", "기억 조작", "번진 잉크 자국",
                "기록의 특정 문장만 물에 번진 것처럼 흐려져 있습니다.",
                "다른 문장은 멀쩡한데 특정 이름과 시각만 지워져 있습니다. 물을 쏟은 실수라기보다, 중요한 내용을 의도적으로 흐리게 만든 흔적처럼 보입니다.",
                List.of("잉크", "삭제", "이름"), "ink");
        addEvidenceItem("wood-dust", "후반 복선", "토끼의 정체", "나무 가루",
                "토끼 주변에서 발견된 미세한 나무 가루.",
                "피 냄새와 섞이지 않는 마른 목각 냄새가 납니다. 토끼가 단순한 동물이 아니라 목각, 공학, 오래된 제작자와 연결되어 있음을 암시합니다.",
                List.of("나무 가루", "목각", "공학"), "wood");
        addEvidenceItem("fading-blood", "초반 의심", "RED 의심", "끊긴 핏자국",
                "바닥에 이어지다 갑자기 끊긴 핏자국.",
                "처음에는 RED가 피를 묻힌 채 이동한 흔적처럼 보입니다. 하지만 핏자국의 간격과 방향이 자연스럽지 않아, 누군가 길을 그려놓은 듯한 느낌이 남습니다.",
                List.of("핏자국", "이동 경로", "연출"), "blood");
        addEvidenceItem("repeated-sentence", "중반 왜곡", "기억 조작", "반복된 문장",
                "낡은 종이에 같은 문장이 여러 번 적혀 있습니다.",
                "\"기억하지 마.\" \"기억하지 마.\" \"기억하지 마.\" 문장은 점점 글씨체가 흐트러져 있고, 마지막 줄은 누군가의 손이 떨린 것처럼 번져 있습니다.",
                List.of("기억하지 마", "반복", "암시"), "memory");
        addEvidenceItem("burned-blueprint", "후반 복선", "토끼의 정체", "태워진 설계도",
                "일부만 남은 기계 설계도.",
                "처음에는 MATCH가 태운 증거처럼 보입니다. 하지만 남은 선은 방화 기록보다 기계와 관절을 설계한 도면에 가깝고, 뛰어난 공학자의 흔적을 남깁니다.",
                List.of("설계도", "기계", "방화 의심"), "blueprint");
        addEvidenceItem("damaged-group-picture", "중반 왜곡", "기억 조작", "훼손된 단체 그림",
                "네 인물이 함께 있는 오래된 그림.",
                "그림 속 네 인물 중 한 얼굴만 심하게 번져 있습니다. 누군가 특정 인물의 기억이나 존재를 지우려 한 것처럼 보이지만, 누구의 얼굴인지는 확정하기 어렵습니다.",
                List.of("단체 그림", "지워진 얼굴", "존재 삭제"), "portrait");
        addEvidenceItem("scratched-watch", "중반 왜곡", "기억 조작", "시계 내부의 긁힌 글자",
                "멈춘 회중시계 안쪽에 작은 글자가 긁혀 있습니다.",
                "안쪽 금속판에는 \"멈춰.\"라는 글자가 남아 있습니다. 시간 정지의 기록처럼 보이지만, 사건의 흐름이나 기억이 강제로 멈춰진 흔적일 수도 있습니다.",
                List.of("멈춰", "회중시계", "흐름 정지"), "watch");
        addEvidenceItem("cracked-doll-eye", "후반 복선", "토끼의 정체", "금 간 인형 눈",
                "오래된 목각 인형의 깨진 눈동자.",
                "깨진 눈동자는 누군가를 감시하던 물건처럼도, 버려진 아이의 일부처럼도 보입니다. PINO와 그의 제작자 사이에 있던 오래된 관계를 조용히 암시합니다.",
                List.of("인형 눈", "목각", "제작자"), "doll");
        addEvidenceItem("wet-wolf-fur", "흔들리는 의심", "RED 의심", "젖은 늑대 털",
                "중앙 홀 난간에 젖은 회색 털 일부가 붙어 있습니다.",
                "처음에는 RED의 늑대인간 정체를 떠올리게 합니다. 하지만 털 끝에는 저택 안 먼지가 아니라 숲의 얼음 결정이 남아 있어, 실제 늑대가 누군가를 살리거나 몰아내려 했던 흔적일 가능성도 남깁니다.",
                List.of("젖은 털", "늑대", "숲의 냄새"), "fur");
        addEvidenceItem("broken-door-handle", "흔들리는 의심", "RED 의심", "날카롭게 부서진 문 손잡이",
                "손으로 뜯긴 것처럼 문 손잡이가 심하게 휘어 있습니다.",
                "괴력으로 문을 부순 흔적처럼 보입니다. 그러나 손잡이는 안쪽에서 바깥쪽으로 꺾여 있어, 누군가 들어가려 한 것이 아니라 필사적으로 빠져나가려 했던 흔적일 수도 있습니다.",
                List.of("문 손잡이", "괴력", "탈출 흔적"), "claw");
        addEvidenceItem("future-note", "불길한 예고", "ALICE 의심", "미래 날짜가 적힌 메모",
                "사건 전 날짜인데 토끼의 죽음을 암시하는 문장이 적혀 있습니다.",
                "\"시계가 멈추면, 토끼도 멈춘다.\" 날짜와 문장은 ALICE가 미래를 알고 있었던 것처럼 보이게 만듭니다. 후반에는 그녀가 환각이 아니라 반복된 시간의 파편을 본 것이라는 해석으로 이어집니다.",
                List.of("미래 날짜", "예지", "반복된 시간"), "paper");
        addEvidenceItem("red-rabbit-drawing", "불길한 예고", "ALICE 의심", "붉게 칠해진 토끼 그림",
                "토끼 그림의 목 부분만 붉게 칠해져 있습니다.",
                "살해 예고처럼 보이지만, 선은 어린아이가 그린 것처럼 떨려 있습니다. 누군가 죽음을 계획했다기보다 같은 장면을 너무 여러 번 보아 손이 먼저 기억한 흔적처럼 보입니다.",
                List.of("토끼 그림", "붉은 목", "반복된 장면"), "portrait");
        addEvidenceItem("half-burned-paper", "지워진 기록", "MATCH 의심", "반쯤 탄 종이",
                "누군가 급하게 태우다 남긴 기록 일부입니다.",
                "남은 문장에는 \"...기억을 남기면 안 돼.\"라고 적혀 있습니다. 처음에는 MATCH의 증거 인멸처럼 보이지만, 후반에는 기억 조작의 피해가 더 퍼지지 않도록 누군가를 보호하려던 행동처럼 읽힙니다.",
                List.of("탄 종이", "기억", "보호와 은폐"), "ash");
        addEvidenceItem("black-ash", "지워진 기록", "MATCH 의심", "바닥의 검은 재",
                "현장 근처 바닥 틈에 검은 재가 남아 있습니다.",
                "성냥 냄새와 비슷해 MATCH를 강하게 의심하게 만듭니다. 하지만 재 속에는 종이보다 얇은 설계도 조각이 섞여 있어, 단순 방화가 아니라 기계 장치의 기록을 없애려 했던 흔적일 수 있습니다.",
                List.of("검은 재", "성냥 냄새", "설계도"), "ash");
        addEvidenceItem("conflicting-times", "기억 충돌", "PINO 의심", "서로 다른 증언 시간",
                "PINO와 대화한 사람들이 서로 다른 시간을 기억합니다.",
                "RED는 11시, MATCH는 11시 20분, ALICE는 이미 시계가 멈춰 있었다고 말합니다. 단순한 혼란처럼 보이지만, 모두가 PINO와 대화한 뒤 시간 감각이 흐려졌다는 공통점이 있습니다.",
                List.of("시간 불일치", "대화 이후", "기억 흔들림"), "watch");
        addEvidenceItem("wall-memory-scratch", "기억 충돌", "PINO 의심", "벽면의 반복된 문장",
                "낡은 벽면에 손톱으로 긁힌 문장이 반복되어 있습니다.",
                "\"기억하지 마.\" 종이에 적힌 문장보다 거칠고 절박합니다. 광기처럼 보이지만, 누군가의 기억을 지우는 말이 벽에까지 새겨질 만큼 반복되었다는 느낌을 줍니다.",
                List.of("기억하지 마", "손톱 자국", "반복 명령"), "memory");
        addEvidenceItem("blurred-face", "기억 충돌", "PINO 의심", "번져 있는 얼굴",
                "단체 그림 속 한 인물의 얼굴만 심하게 번져 있습니다.",
                "처음에는 누군가 그림을 훼손한 것처럼 보입니다. 하지만 주변 물감은 마른 채 얼굴만 흐려져 있어, 물리적 훼손보다 기억 자체가 지워진 상징처럼 남습니다.",
                List.of("번진 얼굴", "존재 삭제", "기억 훼손"), "portrait");
        addEvidenceItem("silent-footprints", "정체의 복선", "토끼의 정체", "방향이 이상한 토끼 발자국",
                "눈 위 발자국은 저택 안으로 들어온 흔적만 있고 나간 흔적이 없습니다.",
                "처음에는 괴담처럼 보입니다. 후반에는 토끼가 단순한 생물이 아니라 시간과 장소를 건너 움직였던 존재였다는 복선으로 이어집니다.",
                List.of("토끼 발자국", "나간 흔적 없음", "시간 이동"), "footprint");
        addEvidenceItem("clock-sound-conflict", "시간 왜곡", "모두의 충돌", "멈춘 시계 소리 증언 충돌",
                "같은 시계를 두고 모두가 다른 소리를 기억합니다.",
                "누군가는 종소리를 들었다고 하고, 누군가는 이미 멈춰 있었다고 하며, 누군가는 시계 자체를 본 적 없다고 합니다. 이 물증은 한 사람보다 사건 전체의 시간이 뒤틀렸다는 느낌을 강화합니다.",
                List.of("종소리", "시간 왜곡", "증언 충돌"), "watch");
        addEvidenceItem("laughing-witness", "증언 충돌", "모두의 충돌", "누군가 계속 웃고 있었다는 기록",
                "토론 전후로 낮은 웃음소리를 들었다는 기록이 남아 있습니다.",
                "RED는 들은 적 없다고 하고, MATCH는 토론 중 들었다고 하며, ALICE는 그게 토끼였다고 주장합니다. PINO는 기억이 안 난다고 말합니다. 웃음소리 하나가 모든 증언을 서로 의심하게 만듭니다.",
                List.of("웃음소리", "증언 충돌", "기억 공백"), "memory");
        addEvidenceItem("rabbit-room-record", "3막 기록", "토끼 기록", "토끼의 방에 남은 기록",
                "낡은 방 한구석에서 발견된 짧은 기록.",
                "아이들은 늘 스스로를 괴물이라 불렀다. 하지만 괴물이라는 말은 대개 자신을 두려워하는 사람들의 입에서 먼저 나온다. 나는 그 아이들에게 다른 이름을 불러주고 싶었다.",
                List.of("토끼 기록", "괴물", "다른 이름"), "paper");
        addEvidenceItem("rabbit-desk-record", "3막 기록", "토끼 기록", "책상 서랍의 기록",
                "잠기지 않은 책상 서랍 안쪽에 접힌 종이.",
                "이번에는 아무도 혼자 두지 않을 거야. 숲에서 길을 잃은 아이도, 불 속에 남겨진 아이도, 이상한 나라에서 돌아오지 못한 아이도, 사람이 되고 싶었던 아이도. 모두 돌아와야 한다.",
                List.of("토끼 기록", "혼자 두지 않음", "구원"), "paper");
        addEvidenceItem("rabbit-workshop-record", "3막 기록", "숨겨진 작업실", "숨겨진 작업실의 기록",
                "기계 부품과 목각 가루 사이에서 발견된 기록.",
                "그 아이는 자신이 사람을 흉내 낸다고 믿었다. 하지만 내가 보기엔 누구보다 사람에 가까웠다. 외로워했고, 질투했고, 사랑받고 싶어 했다. 그게 사람의 마음이 아니라면, 무엇을 사람의 마음이라 부를 수 있을까.",
                List.of("토끼 기록", "그 아이", "사람의 마음"), "wood");

        addPersonalEvidenceHints();
    }

    private void addTestimony(String id, String playerCode, String title, String body) {
        GamePhase phase = phaseForHintId(id);
        hints.put(id, new PersonalHint(id, playerCode, HintType.TESTIMONY, roundLabelFor(phase, "증언"), phase,
                displayNames(title), displayNames(body)));
    }

    private void addEvidenceItem(String id, String revealRound, String category, String name, String shortDescription,
                                 String detail, List<String> keywords, String visual) {
        evidenceItems.put(id, new EvidenceItem(id, revealRound, displayNames(category), displayNames(name),
                displayNames(shortDescription), displayNames(detail),
                keywords.stream().map(this::displayNames).toList(), visual));
    }

    private String displayNames(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("ALICE", "앨리스")
                .replace("PINO", "피노키오")
                .replace("MATCH", "성냥팔이 소녀")
                .replace("RED", "빨간망토");
    }

    private void addPersonalEvidenceHints() {
        List<String> playerCodes = List.of("RED", "ALICE", "PINO", "MATCH");
        List<GamePhase> phases = hintReleasePhases();
        List<EvidenceItem> items = List.copyOf(evidenceItems.values());
        Map<String, String> owners = Map.ofEntries(
                Map.entry("claw-marks", "RED"),
                Map.entry("torn-red-cloth", "RED"),
                Map.entry("scratched-watch", "ALICE"),
                Map.entry("rabbit-room-record", "PINO"),
                Map.entry("rabbit-desk-record", "ALICE"),
                Map.entry("rabbit-workshop-record", "PINO"),
                Map.entry("half-burned-paper", "MATCH"),
                Map.entry("wood-dust", "MATCH"),
                Map.entry("repeated-sentence", "MATCH")
        );
        Map<String, GamePhase> customPhases = Map.ofEntries(
                Map.entry("claw-marks", GamePhase.SECOND_HINT),
                Map.entry("torn-red-cloth", GamePhase.SECOND_HINT),
                Map.entry("scratched-watch", GamePhase.SECOND_HINT),
                Map.entry("wood-dust", GamePhase.THIRD_HINT),
                Map.entry("half-burned-paper", GamePhase.THIRD_HINT),
                Map.entry("repeated-sentence", GamePhase.THIRD_HINT),
                Map.entry("rabbit-room-record", GamePhase.THIRD_HINT),
                Map.entry("rabbit-desk-record", GamePhase.THIRD_HINT),
                Map.entry("rabbit-workshop-record", GamePhase.THIRD_HINT)
        );
        Map<String, String> customBodies = Map.ofEntries(
                Map.entry("claw-marks", """
                        그 자국을 보는 순간, 목덜미의 오래된 상처가 욱신거립니다.

                        하지만 이상하게도 그 자국은 당신이 아는 짐승의 흔적과 조금 다릅니다. 진짜 늑대라면 저렇게 일정하게 긁지 않습니다.

                        누군가 늑대를 흉내 내고 있다면, 그건 당신을 향한 덫일지도 모릅니다.
                        """),
                Map.entry("torn-red-cloth", """
                        찢긴 천을 보는 순간, 도망치던 숲의 냄새가 떠오릅니다.

                        하지만 찢긴 방향이 이상합니다. 짐승에게 물어뜯긴 것이라기보다, 누군가 당신의 망토 색을 알고 일부러 잘라낸 것처럼 느껴집니다.

                        이 천은 당신의 죄가 아니라, 누군가가 당신에게 씌우려는 얼굴일지도 모릅니다.
                        """),
                Map.entry("scratched-watch", """
                        그 그림과 글자를 볼 때마다, 초침 소리가 들리는 것 같습니다.

                        하지만 이상한 점은 그 소리가 시계에서 나는 것이 아니라 벽 안쪽 어딘가에서 나는 것처럼 느껴진다는 것입니다.

                        누군가 시간을 멈춘 게 아니라, 시간이 멈춘 것처럼 보이게 만들고 있는 것 같았습니다.
                        """),
                Map.entry("wood-dust", """
                        사건 직후 당신은 PINO의 손끝을 보았습니다.

                        피가 아니라 아주 미세한 나무 가루가 묻어 있었습니다. 말하려 했지만 입을 다물었습니다. 당신도 그 직후 정체를 알 수 없는 설계도 같은 종이를 태웠기 때문입니다.

                        누군가를 의심하기엔, 당신 역시 너무 많은 것을 숨기고 있었습니다.
                        """),
                Map.entry("half-burned-paper", """
                        종이 끝부분은 분명 누군가 급하게 태우려다 멈춘 흔적입니다.

                        그을린 부분을 보는 순간, 당신이 태웠던 설계도도 떠오릅니다. 관절처럼 꺾인 선, 실이 지나가는 위치, 누군가를 움직이기 위해 그려진 것 같은 구조.

                        그 종이를 남겨두면 누군가 또 망가질 것 같았습니다.
                        """),
                Map.entry("repeated-sentence", """
                        반복된 문장은 단순한 낙서처럼 보이지 않습니다.

                        종이 끝부분은 누군가 급히 태우려다 멈춘 흔적이고, 그을린 냄새는 당신이 숨긴 설계도 냄새와 닮아 있습니다.

                        기억을 남기면 안 된다는 말은 협박일까요, 아니면 누군가를 보호하려는 절박한 부탁일까요.
                        """),
                Map.entry("rabbit-room-record", """
                        “이번에는 아무도 혼자 두지 않을 거야.”

                        그 문장을 읽는 순간, 당신은 자신도 모르게 손을 움켜쥡니다. 다정해서가 아니라, 너무 늦게 도착한 말처럼 느껴졌기 때문입니다.

                        당신은 혼자 남기 싫었습니다. 하지만 토끼는 모두를 혼자 두지 않으려 했습니다. 그 차이가 견딜 수 없습니다.
                        """),
                Map.entry("rabbit-desk-record", """
                        이 기록은 환각처럼 보이지 않습니다.

                        글자는 떨리고 있지만, 문장마다 누군가를 잃어본 사람의 숨이 남아 있습니다. 토끼는 누군가를 심판하려 한 게 아니라, 돌아오지 못한 아이들을 다시 부르려 했던 것 같습니다.
                        """),
                Map.entry("rabbit-workshop-record", """
                        “그 아이는 누구보다 사람에 가까웠다.”

                        그 문장을 읽는 순간, 당신은 웃으려다 실패합니다. 불쌍하다는 말, 구원이라는 말, 버려졌다는 말, 괴물이라는 말, 특별하지 않았다는 말, 혼자 남았다는 말이 나오면 시선을 피하세요.

                        누군가 “토끼는 너를 무서워하지 않았다”고 말한다면, 잠깐 말을 멈추고 손끝을 만지세요. 필요하다면 아주 낮게 “그만해”라고 말해도 됩니다.
                        """)
        );
        for (int index = 0; index < items.size(); index++) {
            EvidenceItem item = items.get(index);
            String playerCode = owners.getOrDefault(item.id(), playerCodes.get(index % playerCodes.size()));
            GamePhase phase = customPhases.getOrDefault(item.id(), phases.get((index / playerCodes.size()) % phases.size()));
            String body = customBodies.getOrDefault(item.id(), item.shortDescription() + "\n\n" + item.detail());
            hints.put("evidence-" + item.id(), new PersonalHint(
                    "evidence-" + item.id(),
                    playerCode,
                    HintType.EVIDENCE,
                    roundLabelFor(phase, "물증"),
                    phase,
                    item.name(),
                    body
            ));
        }
    }

    private GamePhase phaseForHintId(String id) {
        if (id.endsWith("t1") || id.endsWith("t2")) {
            return GamePhase.FIRST_HINT;
        }
        if (id.endsWith("t3") || id.endsWith("t4")) {
            return GamePhase.SECOND_HINT;
        }
        return GamePhase.THIRD_HINT;
    }

    private List<GamePhase> hintReleasePhases() {
        return List.of(GamePhase.FIRST_HINT, GamePhase.SECOND_HINT, GamePhase.THIRD_HINT);
    }

    private String roundLabelFor(GamePhase phase, String typeLabel) {
        int round = hintReleasePhases().indexOf(phase) + 1;
        return round + "차 힌트 " + typeLabel;
    }

    private void loadExternalStories() {
        Map<String, Path> files = Map.of(
                "ALICE", Path.of("C:/Users/seoc1/OneDrive/바탕 화면/앨리스 이야기.txt"),
                "PINO", Path.of("C:/Users/seoc1/OneDrive/바탕 화면/피노키오 이야기.txt"),
                "MATCH", Path.of("C:/Users/seoc1/OneDrive/바탕 화면/성냥팔이소녀이야기.txt"),
                "RED", Path.of("C:/Users/seoc1/OneDrive/바탕 화면/빨간망토 이야기.txt")
        );
        files.forEach((code, path) -> {
            try {
                StoryContent content = parseStoryFile(Files.readString(path, StandardCharsets.UTF_8));
                storyContents.put(code, content);
                Player player = players.get(code);
                if (player != null) {
                    players.put(code, new Player(
                            player.code(),
                            player.name(),
                            player.role(),
                            player.publicDescription(),
                            content.personalStory(),
                            content.secretAndActingHint(),
                            player.color()
                    ));
                }
            } catch (IOException ignored) {
                // Keep the built-in story when the local draft file is unavailable.
            }
        });
    }

    private StoryContent parseStoryFile(String source) {
        String normalized = source.replace("\r\n", "\n").replace("\r", "\n").trim();
        int storyIndex = normalized.indexOf("개인 이야기");
        int actingIndex = normalized.indexOf("연기 힌트");
        String prologueBlock = storyIndex >= 0 ? normalized.substring(0, storyIndex) : normalized;
        String storyBlock = storyIndex >= 0 && actingIndex >= 0
                ? normalized.substring(storyIndex, actingIndex)
                : "";
        String actingBlock = actingIndex >= 0 ? normalized.substring(actingIndex) : "";

        String title = prologueBlock.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("\""))
                .findFirst()
                .orElse("초대장");
        List<String> prologueParagraphs = paragraphs(prologueBlock).stream()
                .filter(paragraph -> !paragraph.startsWith("\""))
                .filter(paragraph -> !paragraph.equals(title))
                .toList();

        int secretIndex = storyBlock.indexOf("비밀 정보");
        String personalStory = secretIndex >= 0 ? storyBlock.substring(0, secretIndex) : storyBlock;
        String secret = secretIndex >= 0 ? storyBlock.substring(secretIndex) : "";
        String secretAndActingHint = (secret + "\n\n" + actingBlock).trim();
        return new StoryContent(title, prologueParagraphs, cleanSectionTitle(personalStory), cleanSectionTitle(secretAndActingHint));
    }

    private List<String> paragraphs(String text) {
        return java.util.Arrays.stream(text.split("\\n\\s*\\n"))
                .map(String::trim)
                .filter(paragraph -> !paragraph.isBlank())
                .collect(Collectors.toList());
    }

    private String cleanSectionTitle(String text) {
        return text.lines()
                .map(String::trim)
                .filter(line -> !line.startsWith("\""))
                .collect(Collectors.joining("\n"))
                .trim();
    }

    public Optional<Player> login(String code, HttpSession session) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        Player player = players.get(normalized);
        if (player == null) {
            return Optional.empty();
        }
        session.setAttribute(PLAYER_CODE, normalized);
        if (!normalized.equals("ADMIN")) {
            enteredPlayers.put(normalized, Instant.now());
        }
        return Optional.of(player);
    }

    public Optional<Player> currentPlayer(HttpSession session) {
        Object code = session.getAttribute(PLAYER_CODE);
        return code == null ? Optional.empty() : Optional.ofNullable(players.get(code.toString()));
    }

    public Optional<Player> playerByCode(String code) {
        return Optional.ofNullable(players.get(code));
    }

    public void leave(HttpSession session) {
        currentPlayer(session).ifPresent(player -> enteredPlayers.remove(player.code()));
        session.invalidate();
    }

    public boolean isPrologueDone(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(PROLOGUE_DONE));
    }

    public void finishPrologue(HttpSession session) {
        session.setAttribute(PROLOGUE_DONE, true);
    }

    public GameState state() {
        syncTimer();
        return state;
    }

    public List<GamePhase> phases() {
        return List.of(GamePhase.values());
    }

    public void nextPhase() {
        GamePhase[] values = GamePhase.values();
        int next = Math.min(state.getPhase().ordinal() + 1, values.length - 1);
        state.setPhase(values[next]);
    }

    public void previousPhase() {
        GamePhase[] values = GamePhase.values();
        int previous = Math.max(state.getPhase().ordinal() - 1, 0);
        state.setPhase(values[previous]);
    }

    public void setTimer(int minutes, int seconds) {
        state.setTimerRunning(false);
        state.setTimerEndsAt(null);
        state.setTimerSeconds(Math.max(0, minutes * 60 + seconds));
    }

    public void resetTimer() {
        state.setTimerRunning(false);
        state.setTimerEndsAt(null);
        state.setTimerSeconds(0);
    }

    public void startTimer() {
        syncTimer();
        state.setTimerEndsAt(Instant.now().plusSeconds(state.getTimerSeconds()));
        state.setTimerRunning(state.getTimerSeconds() > 0);
    }

    public void stopTimer() {
        syncTimer();
        state.setTimerRunning(false);
        state.setTimerEndsAt(null);
    }

    public void unlockPersonalStories() {
        playableCharacters().stream()
                .map(Player::code)
                .forEach(unlockedPersonalStoryCodes::add);
    }

    public void unlockPersonalStory(String playerCode) {
        Player player = players.get(playerCode == null ? "" : playerCode.trim().toUpperCase());
        if (player != null && !player.code().equals("ADMIN")) {
            unlockedPersonalStoryCodes.add(player.code());
        }
    }

    public void openInvestigationScenes() {
        openedInvestigationActs.add(1);
        state.setInvestigationOpen(true);
    }

    public void openInvestigationAct(int act) {
        if (act < 1 || act > 3) {
            return;
        }
        openedInvestigationActs.add(act);
        state.setInvestigationOpen(true);
    }

    public boolean isPersonalStoryRevealedFor(Player player) {
        return player != null && !player.code().equals("ADMIN") && unlockedPersonalStoryCodes.contains(player.code());
    }

    public void revealHint(String hintId) {
        PersonalHint hint = hints.get(hintId);
        if (hint != null && isHintRevealable(hint)) {
            revealedHintIds.add(hintId);
        }
    }

    public void releaseHintRound(GamePhase phase) {
        if (!hintReleasePhases().contains(phase) || !releasedHintPhases.add(phase)) {
            return;
        }
        hints.values().stream()
                .filter(hint -> hint.phase() == phase)
                .map(PersonalHint::id)
                .forEach(revealedHintIds::add);
        state.setPhase(phase);
    }

    public List<HintRoundStatus> hintRoundStatuses() {
        return hintReleasePhases().stream()
                .map(phase -> new HintRoundStatus(
                        phase,
                        hintReleasePhases().indexOf(phase) + 1,
                        releasedHintPhases.contains(phase),
                        hints.values().stream().filter(hint -> hint.phase() == phase).count()
                ))
                .collect(Collectors.toList());
    }

    public int releasedHintRoundCount() {
        return releasedHintPhases.size();
    }

    public void revealEvidence(String evidenceId) {
        if (evidenceItems.containsKey(evidenceId)) {
            revealedEvidenceIds.add(evidenceId);
        }
    }

    public List<EvidenceStatus> evidenceStatuses() {
        return evidenceItems.values().stream()
                .map(evidence -> new EvidenceStatus(evidence, revealedEvidenceIds.contains(evidence.id())))
                .collect(Collectors.toList());
    }

    public List<EvidenceItem> publicEvidenceItems() {
        return evidenceItems.values().stream()
                .filter(evidence -> revealedEvidenceIds.contains(evidence.id()))
                .collect(Collectors.toList());
    }

    public List<InvestigationScene> investigationScenes() {
        return List.of(
                new InvestigationScene(
                        "central-hall",
                        "중앙 홀 기록",
                        "멈춘 시계와 핏자국이 처음으로 모두의 기억을 갈라놓은 장소.",
                        List.of("11시 47분", "벽의 발톱 자국", "끊긴 핏자국", "젖은 늑대 털"),
                        List.of(
                                "RED는 이곳의 흔적이 자신을 향해 너무 친절하게 놓여 있다고 느낍니다.",
                                "ALICE는 시계 소리가 벽 안쪽에서 들렸다고 말할 수 있습니다.",
                                "MATCH는 피 냄새보다 누군가 급히 지나간 젖은 연기 냄새를 먼저 떠올립니다.",
                                "PINO는 이 장소의 시간이 한 번이 아니라 여러 번 말해진 것처럼 느낍니다."
                        ),
                        "이 장면의 핵심은 범행 흔적보다, 모두가 같은 시간을 다르게 기억한다는 사실입니다."
                ),
                new InvestigationScene(
                        "alice-room",
                        "앨리스의 방 기록",
                        "그림과 낙서, 들리지 않는 목소리가 남아 있는 작은 방.",
                        List.of("붉게 칠해진 토끼 그림", "미래 날짜가 적힌 메모", "번져 있는 얼굴", "이름을 부르는 목소리"),
                        List.of(
                                "ALICE는 자신이 환각을 본 것이 아니라 너무 이른 진실을 본 것일지도 모른다고 느낍니다.",
                                "다른 플레이어는 이 기록을 살해 예고나 광기의 흔적으로 의심할 수 있습니다.",
                                "PINO는 이름을 부르는 목소리 이야기가 나오면 짧게 숨을 멈출 수 있습니다.",
                                "이 방의 기록은 진실을 말하지만, 말하는 방식이 너무 이상해서 쉽게 믿기 어렵습니다."
                        ),
                        "이 장면은 앨리스가 이상한 아이인지, 진실을 가장 먼저 들은 사람인지 흔들리게 만듭니다."
                ),
                new InvestigationScene(
                        "rabbit-room",
                        "토끼의 방 기록",
                        "피해자의 방처럼 보이지만, 사실 모두를 구하려던 흔적이 남아 있는 장소.",
                        List.of("토끼의 방에 남은 기록", "책상 서랍의 기록", "방향이 이상한 토끼 발자국", "끊어진 실 조각"),
                        List.of(
                                "기록은 토끼가 네 사람을 괴물로 보지 않았다는 사실을 암시합니다.",
                                "하지만 정체를 직접 밝히지는 않습니다. 진실은 엔딩까지 남겨두어야 합니다.",
                                "PINO는 이 방의 문장을 읽을수록 버려질 공포와 죄책감이 함께 올라옵니다.",
                                "다른 플레이어들은 토끼가 단순 피해자가 아니라 사건을 설계한 존재일지도 모른다고 의심할 수 있습니다."
                        ),
                        "이 장면은 범인을 찾는 장면이 아니라, 토끼가 왜 이들을 모았는지 묻는 장면입니다."
                ),
                new InvestigationScene(
                        "hidden-workshop",
                        "숨겨진 작업실 기록",
                        "목각 가루와 설계도, 실이 남아 있는 가장 조용하고 위험한 장소.",
                        List.of("나무 가루", "태워진 설계도", "금 간 인형 눈", "반복된 문장"),
                        List.of(
                                "MATCH는 자신이 태운 것이 단순한 종이가 아니라 누군가를 움직이는 설계였을지도 모른다고 느낍니다.",
                                "PINO는 이곳의 물증이 자신을 직접 가리키지 않는데도 몸이 먼저 굳습니다.",
                                "RED에게는 이 장소가 자신을 범인처럼 꾸민 손의 존재를 떠올리게 합니다.",
                                "ALICE에게는 실과 나무가 살아 있는 기억처럼 보일 수 있습니다."
                        ),
                        "이 장면은 PINO를 향한 의심을 키우지만, 동시에 그 역시 만들어진 피해자였음을 남깁니다."
                )
        );
    }

    public List<InvestigationSceneView> investigationScenesFor(Player player) {
        return investigationScenes().stream()
                .map(scene -> new InvestigationSceneView(
                        scene.id(),
                        scene.title(),
                        scene.summary(),
                        scene.clues(),
                        sceneInterpretationsFor(scene.id(), player.code())
                ))
                .collect(Collectors.toList());
    }

    private List<String> sceneInterpretationsFor(String sceneId, String playerCode) {
        return switch (sceneId + ":" + playerCode) {
            case "central-hall:RED" -> List.of("발톱 자국이 너무 일정합니다. 누군가 당신을 닮은 괴물을 현장에 남겨둔 것 같습니다.");
            case "central-hall:ALICE" -> List.of("시계 소리는 시계에서 난 것이 아니라 벽 안쪽에서 울렸던 것 같습니다.");
            case "central-hall:PINO" -> List.of("이 장소의 시간은 한 번이 아니라 여러 번 말해진 것처럼 머릿속에서 겹칩니다.");
            case "central-hall:MATCH" -> List.of("피 냄새보다 젖은 연기 냄새가 먼저 떠오릅니다. 누군가 지나간 뒤에 남은 냄새였습니다.");
            case "alice-room:RED" -> List.of("앨리스의 그림은 예고처럼 보이지만, 이상하게도 누군가 이미 본 장면을 따라 그린 것 같습니다.");
            case "alice-room:ALICE" -> List.of("당신은 환각을 본 것이 아니라 너무 이른 진실을 들은 것일지도 모릅니다.");
            case "alice-room:PINO" -> List.of("이름을 부르는 목소리 이야기가 나오면, 당신은 자신도 모르게 숨을 멈추게 됩니다.");
            case "alice-room:MATCH" -> List.of("앨리스가 장난처럼 말한 문장마다 손끝이 차가웠습니다. 거짓말을 하는 사람의 손은 아니었습니다.");
            case "rabbit-room:RED" -> List.of("토끼의 기록은 당신을 괴물로 부르지 않습니다. 그래서 더 믿기 어렵습니다.");
            case "rabbit-room:ALICE" -> List.of("이 방의 문장들은 환각처럼 흔들리지 않습니다. 누군가 정말로 모두를 돌아오게 하려 했습니다.");
            case "rabbit-room:PINO" -> List.of("혼자 두지 않겠다는 말이 너무 늦게 도착한 말처럼 느껴집니다.");
            case "rabbit-room:MATCH" -> List.of("불 속에서도 누군가 자신을 기다렸다는 문장이 남아 있습니다. 그게 더 무섭습니다.");
            case "hidden-workshop:RED" -> List.of("이곳의 물건들은 당신의 흔적을 꾸며낸 손이 실제로 있었을지도 모른다는 생각을 남깁니다.");
            case "hidden-workshop:ALICE" -> List.of("실과 나무가 살아 있는 기억처럼 보입니다. 누군가 움직였고, 누군가 움직여졌습니다.");
            case "hidden-workshop:PINO" -> List.of("물증은 당신의 이름을 말하지 않는데도, 몸이 먼저 굳습니다.");
            case "hidden-workshop:MATCH" -> List.of("당신이 태운 것은 종이가 아니라 누군가를 움직이는 방법이었을지도 모릅니다.");
            default -> List.of();
        };
    }

    public List<InvestigationActStatus> investigationActStatuses() {
        return List.of(1, 2, 3).stream()
                .map(act -> new InvestigationActStatus(
                        act,
                        act + "막 기록",
                        openedInvestigationActs.contains(act),
                        investigationEvidenceItems().stream().filter(item -> item.act() == act).count()
                ))
                .collect(Collectors.toList());
    }

    public List<InvestigationLocationView> investigationLocationsFor(Player player) {
        Map<String, InvestigationLocationBuilder> locations = new LinkedHashMap<>();
        investigationEvidenceItems().stream()
                .filter(item -> openedInvestigationActs.contains(item.act()))
                .forEach(item -> {
                    String key = item.act() + ":" + item.locationName();
                    InvestigationLocationBuilder builder = locations.computeIfAbsent(key, ignored ->
                            new InvestigationLocationBuilder(
                                    item.act(),
                                    "act-" + item.act() + "-" + slug(item.locationName()),
                                    item.locationName(),
                                    item.act() + "막에서 펼쳐진 " + item.locationName() + "의 남겨진 흔적"
                            ));
                    builder.add(new InvestigationEvidenceView(
                            item.id(),
                            item.act(),
                            item.title(),
                            item.locationName(),
                            item.detailLocation(),
                            item.commonDescription(),
                            item.interpretations().getOrDefault(player.code(), ""),
                            item.recalledLine()
                    ));
                });
        return locations.values().stream()
                .map(InvestigationLocationBuilder::build)
                .collect(Collectors.toList());
    }

    public List<InvestigationLocationStatus> investigationLocationStatusesFor(Player player) {
        return investigationLocationDefinitions().stream()
                .filter(location -> openedInvestigationActs.contains(location.act()))
                .map(location -> {
                    LocationOccupancy occupancy = investigationOccupancies.get(location.id());
                    Player occupant = occupancy == null ? null : players.get(occupancy.playerCode());
                    boolean occupiedBySelf = occupancy != null && occupancy.playerCode().equals(player.code());
                    boolean canEnter = occupancy == null || occupiedBySelf;
                    return new InvestigationLocationStatus(
                            location.id(),
                            location.act(),
                            location.name(),
                            location.description(),
                            true,
                            occupancy != null,
                            occupiedBySelf,
                            canEnter,
                            occupant == null ? "" : occupant.name(),
                            occupancy == null ? null : occupancy.occupiedAt(),
                            evidenceSummariesFor(location.id(), player)
                    );
                })
                .collect(Collectors.toList());
    }

    public InvestigationActionResult enterInvestigationLocation(Player player, String locationId) {
        InvestigationLocation location = investigationLocation(locationId);
        if (location == null || !openedInvestigationActs.contains(location.act())) {
            return new InvestigationActionResult(false, "아직 공개되지 않은 장면입니다.");
        }
        LocationOccupancy occupancy = investigationOccupancies.get(locationId);
        if (occupancy == null) {
            investigationOccupancies.entrySet().removeIf(entry -> entry.getValue().playerCode().equals(player.code()));
            investigationOccupancies.put(locationId, new LocationOccupancy(locationId, player.code(), Instant.now()));
            return new InvestigationActionResult(true, location.name() + " 조사를 시작합니다.");
        }
        if (occupancy.playerCode().equals(player.code())) {
            return new InvestigationActionResult(true, location.name() + "에 다시 들어갑니다.");
        }
        Player occupant = players.get(occupancy.playerCode());
        String name = occupant == null ? "다른 플레이어" : occupant.name();
        return new InvestigationActionResult(false, "현재 " + name + "가 이 장면을 조사 중입니다.");
    }

    public InvestigationActionResult leaveInvestigationLocation(Player player, String locationId) {
        LocationOccupancy occupancy = investigationOccupancies.get(locationId);
        if (occupancy == null) {
            return new InvestigationActionResult(true, "이미 비어 있는 장면입니다.");
        }
        if (!occupancy.playerCode().equals(player.code())) {
            return new InvestigationActionResult(false, "현재 조사자만 조사를 종료할 수 있습니다.");
        }
        investigationOccupancies.remove(locationId);
        return new InvestigationActionResult(true, "조사를 종료했습니다.");
    }

    public void forceReleaseInvestigationLocation(String locationId) {
        investigationOccupancies.remove(locationId);
    }

    public EvidenceDetailResult discoverInvestigationEvidence(Player player, String evidenceId) {
        InvestigationEvidence evidence = investigationEvidenceById(evidenceId);
        if (evidence == null) {
            return new EvidenceDetailResult(false, "존재하지 않는 증거입니다.", null);
        }
        String locationId = locationIdFor(evidence);
        if (!publishedEvidence.containsKey(evidenceId)) {
            LocationOccupancy occupancy = investigationOccupancies.get(locationId);
            if (occupancy == null || !occupancy.playerCode().equals(player.code())) {
                return new EvidenceDetailResult(false, "이 장면을 조사 중인 플레이어만 발견할 수 있습니다.", null);
            }
            playerEvidenceStates.putIfAbsent(playerEvidenceKey(player.code(), evidenceId),
                    new PlayerEvidenceState(player.code(), evidenceId, EvidenceDiscoveryStatus.PRIVATE_FOUND, Instant.now(), null));
        }
        return new EvidenceDetailResult(true, "증거를 펼쳤습니다.", evidenceDetailFor(evidence, player));
    }

    public EvidenceDetailResult evidenceDetail(Player player, String evidenceId) {
        InvestigationEvidence evidence = investigationEvidenceById(evidenceId);
        if (evidence == null) {
            return new EvidenceDetailResult(false, "존재하지 않는 증거입니다.", null);
        }
        if (!publishedEvidence.containsKey(evidenceId)
                && !playerEvidenceStates.containsKey(playerEvidenceKey(player.code(), evidenceId))) {
            return new EvidenceDetailResult(false, "아직 발견하지 않은 증거입니다.", null);
        }
        return new EvidenceDetailResult(true, "증거를 펼쳤습니다.", evidenceDetailFor(evidence, player));
    }

    public InvestigationActionResult publishInvestigationEvidence(Player player, String evidenceId) {
        InvestigationEvidence evidence = investigationEvidenceById(evidenceId);
        if (evidence == null) {
            return new InvestigationActionResult(false, "존재하지 않는 증거입니다.");
        }
        String key = playerEvidenceKey(player.code(), evidenceId);
        PlayerEvidenceState state = playerEvidenceStates.get(key);
        if (state == null && !publishedEvidence.containsKey(evidenceId)) {
            return new InvestigationActionResult(false, "먼저 이 증거를 발견해야 공개할 수 있습니다.");
        }
        if (publishedEvidence.containsKey(evidenceId)) {
            return new InvestigationActionResult(true, "이미 모두의 기록에 올라간 증거입니다.");
        }
        Instant now = Instant.now();
        playerEvidenceStates.put(key, new PlayerEvidenceState(player.code(), evidenceId, EvidenceDiscoveryStatus.PUBLIC, state.discoveredAt(), now));
        publishedEvidence.put(evidenceId, new PublishedEvidence(evidenceId, player.code(), locationIdFor(evidence), now));
        return new InvestigationActionResult(true, evidence.title() + "을 모두의 기록에 공개했습니다.");
    }

    public InvestigationActionResult adminPublishInvestigationEvidence(String evidenceId) {
        InvestigationEvidence evidence = investigationEvidenceById(evidenceId);
        if (evidence == null) {
            return new InvestigationActionResult(false, "존재하지 않는 증거입니다.");
        }
        publishedEvidence.putIfAbsent(evidenceId, new PublishedEvidence(evidenceId, "ADMIN", locationIdFor(evidence), Instant.now()));
        return new InvestigationActionResult(true, evidence.title() + "을 강제로 공개했습니다.");
    }

    public List<PlayerEvidenceView> privateEvidenceFor(Player player) {
        return playerEvidenceStates.values().stream()
                .filter(state -> state.playerCode().equals(player.code()))
                .filter(state -> state.status() == EvidenceDiscoveryStatus.PRIVATE_FOUND)
                .map(state -> playerEvidenceView(state, player))
                .collect(Collectors.toList());
    }

    public List<PlayerEvidenceView> publicInvestigationEvidenceFor(Player player) {
        return publishedEvidence.values().stream()
                .map(item -> {
                    InvestigationEvidence evidence = investigationEvidenceById(item.evidenceId());
                    Player publisher = players.get(item.publishedByPlayerCode());
                    return new PlayerEvidenceView(
                            evidence.id(),
                            evidence.title(),
                            evidence.locationName(),
                            evidence.detailLocation(),
                            item.sourceLocationId(),
                            publisher == null ? "관리자" : publisher.name(),
                            item.publishedAt(),
                            EvidenceDiscoveryStatus.PUBLIC
                    );
                })
                .collect(Collectors.toList());
    }

    public List<AdminLocationStatus> adminLocationStatuses() {
        return investigationLocationDefinitions().stream()
                .map(location -> {
                    LocationOccupancy occupancy = investigationOccupancies.get(location.id());
                    Player occupant = occupancy == null ? null : players.get(occupancy.playerCode());
                    return new AdminLocationStatus(
                            location,
                            openedInvestigationActs.contains(location.act()),
                            occupant,
                            occupancy == null ? null : occupancy.occupiedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<AdminPrivateEvidenceStatus> adminPrivateEvidenceStatuses() {
        return playableCharacters().stream()
                .map(player -> new AdminPrivateEvidenceStatus(player, privateEvidenceFor(player)))
                .collect(Collectors.toList());
    }

    private List<InvestigationEvidenceSummary> evidenceSummariesFor(String locationId, Player player) {
        return investigationEvidenceItems().stream()
                .filter(evidence -> locationIdFor(evidence).equals(locationId))
                .map(evidence -> {
                    boolean publicItem = publishedEvidence.containsKey(evidence.id());
                    boolean privateFound = playerEvidenceStates.containsKey(playerEvidenceKey(player.code(), evidence.id()));
                    return new InvestigationEvidenceSummary(
                            evidence.id(),
                            evidence.title(),
                            evidence.detailLocation(),
                            publicItem,
                            privateFound
                    );
                })
                .collect(Collectors.toList());
    }

    private PlayerEvidenceView playerEvidenceView(PlayerEvidenceState state, Player viewer) {
        InvestigationEvidence evidence = investigationEvidenceById(state.evidenceId());
        return new PlayerEvidenceView(
                evidence.id(),
                evidence.title(),
                evidence.locationName(),
                evidence.detailLocation(),
                locationIdFor(evidence),
                viewer.name(),
                state.discoveredAt(),
                state.status()
        );
    }

    private EvidenceDetailView evidenceDetailFor(InvestigationEvidence evidence, Player player) {
        PublishedEvidence published = publishedEvidence.get(evidence.id());
        Player publisher = published == null ? null : players.get(published.publishedByPlayerCode());
        return new EvidenceDetailView(
                evidence.id(),
                evidence.title(),
                evidence.locationName(),
                evidence.detailLocation(),
                evidence.commonDescription(),
                evidence.interpretations().getOrDefault(player.code(), ""),
                evidence.recalledLine(),
                published != null,
                publisher == null ? "" : publisher.name()
        );
    }

    private InvestigationEvidence investigationEvidenceById(String evidenceId) {
        return investigationEvidenceItems().stream()
                .filter(evidence -> evidence.id().equals(evidenceId))
                .findFirst()
                .orElse(null);
    }

    private InvestigationLocation investigationLocation(String locationId) {
        return investigationLocationDefinitions().stream()
                .filter(location -> location.id().equals(locationId))
                .findFirst()
                .orElse(null);
    }

    private String locationIdFor(InvestigationEvidence evidence) {
        return "act-" + evidence.act() + "-" + slug(evidence.locationName());
    }

    private String playerEvidenceKey(String playerCode, String evidenceId) {
        return playerCode + ":" + evidenceId;
    }

    private List<InvestigationLocation> investigationLocationDefinitions() {
        Map<String, InvestigationLocation> locations = new LinkedHashMap<>();
        investigationEvidenceItems().forEach(evidence -> {
            String id = locationIdFor(evidence);
            locations.putIfAbsent(id, new InvestigationLocation(
                    id,
                    evidence.act(),
                    evidence.locationName(),
                    sceneDescription(evidence.locationName())
            ));
        });
        return List.copyOf(locations.values());
    }

    private String sceneDescription(String locationName) {
        return switch (locationName) {
            case "중앙 홀" -> "핏자국과 멈춘 시간이 가장 먼저 남겨진 장면.";
            case "토끼의 방" -> "버려진 아이들이 잠시 쉬어가던 작은 방.";
            case "복도" -> "시체가 놓여 있던 자리와 발소리의 기억이 겹치는 길.";
            case "앨리스의 방" -> "멈춘 시계와 현실의 틈이 벽에 남은 방.";
            case "멈춘 회중시계 내부" -> "시간보다 기억이 먼저 멈춘 듯한 작은 장치 안.";
            case "멈춘 회중시계 내부 금속판" -> "멈춰 달라는 말이 금속 안쪽에 긁힌 곳.";
            case "숨겨진 작업실" -> "실과 나무 가루, 태워진 기록이 남은 가장 조용한 방.";
            default -> "누군가의 기억이 접힌 채 남아 있는 장면.";
        };
    }

    public List<InvestigationEvidence> investigationEvidenceItems() {
        return List.of(
                investigationEvidence("wet-wolf-fur", 1, "젖은 늑대 털", "중앙 홀", "긴 식탁 아래",
                        """
                                식탁 아래,
                                물기가 마르지 않은 회색 털 몇 올이 떨어져 있다.

                                털 끝에는 눈 녹은 물방울이 남아 있고,
                                저택 안 먼지와는 다른 흙냄새가 섞여 있다.

                                처음 보면 짐승이 남긴 흔적처럼 보인다.

                                하지만 자세히 보면,
                                일부 털은 가느다란 실 조각에 얽혀 있다.

                                털 일부는 칼날 같은 것으로 잘린 흔적이 남아 있으며,
                                바닥에 자연스럽게 떨어진 것이라기보다
                                누군가 손으로 급히 흩뿌린 것처럼 보인다.
                                """,
                        """
                                털 냄새를 맡는 순간,
                                눈 덮인 숲속에서 들리던 거친 숨소리가 떠오른다.

                                젖은 털 냄새.
                                눈을 짓밟는 발소리.
                                가까워지던 낮은 숨.

                                무언가 자신을 쫓아온 것 같기도 하고,
                                반대로 자신이 누군가를 쫓고 있었던 것 같기도 하다.

                                목덜미의 오래된 상처가 아주 짧게 욱신거린다.
                                """,
                        """
                                앨리스는 털보다
                                그 사이에 얽힌 실 조각이 먼저 눈에 들어온다.

                                짐승의 흔적인데도,
                                어쩐지 장난감 무대 위에 올려진 소품처럼 느껴진다.

                                진짜 늑대의 흔적이라기보다,
                                누군가 “늑대가 있었던 장면”을 따라 꾸며놓은 것 같다.

                                아까 자신이 보았던
                                토끼 같기도 하고,
                                사람 같기도 했던 흔적이 떠오른다.
                                """,
                        """
                                실 끝에 묻은 나무 가루를 보는 순간,
                                머릿속에서 무언가 끊어지는 감각이 스친다.

                                그것은 털의 흔적보다
                                실이 억지로 뜯겨나간 흔적처럼 보인다.

                                누군가 짐승을 흉내 내려 한 것인지,
                                아니면 누군가의 몸에서 무언가가 떨어져 나온 것인지는 알 수 없다.
                                """,
                        """
                                성냥팔이 소녀는 젖은 털 냄새보다,
                                털 끝에 묻은 아주 희미한 탄내를 먼저 느낀다.

                                불에 탄 냄새라고 하기엔 약하지만,
                                불 가까이에 오래 두었던 물건에서 나는 냄새와 비슷하다.

                                누군가 불 가까이에서 이것을 말렸거나,
                                불에 그을린 손으로 만졌던 것 같다.
                                """,
                        """
                                아까 빨간망토가 낮게 말했던 것이 떠오른다.

                                “나는 가끔 모르겠어.

                                내가 쫓기고 있는 건지,
                                아니면 누군가를 쫓고 있는 건지.”
                                """),
                investigationEvidence("claw-marks", 1, "벽의 발톱 자국", "중앙 홀", "출입문 옆 벽면",
                        """
                                중앙 홀 벽면에는 누군가 깊게 긁어낸 듯한 흔적이 남아 있다.

                                처음 보면 짐승이 벽을 찢은 것처럼 보인다.

                                하지만 긁힌 깊이가 지나치게 일정하고, 간격도 거의 비슷하며, 시작 방향도 어색하다.

                                마치 누군가 일부러 “늑대의 흔적”을 흉내 낸 것처럼 보인다.

                                긁힌 자국 주변에는 벽지가 아주 얇게 벗겨져 있으며,
                                가까이 보면 손으로 여러 번 덧그은 듯한 흔적도 남아 있다.
                                """,
                        """
                                그 자국을 보는 순간, 목덜미 상처가 욱신거린다.

                                눈밭 위로 쓰러졌던 순간, 등을 덮치던 무게,
                                목덜미를 파고들던 고통이 떠오른다.

                                하지만 이상하다.

                                진짜 늑대라면 저렇게 일정하게 긁지 않았을 것이다.

                                누군가 자신의 기억을 흉내 내고 있다면,
                                그건 자신을 향한 덫일지도 모른다.
                                """,
                        """
                                앨리스는 자국의 간격을 한참 바라본다.

                                진짜 짐승이라면 저렇게 긁지 않았을 거라는 생각이 든다.

                                마치 누군가 그림책 속 괴물을 보고,
                                괴물이 남겼을 법한 흔적을 따라 그린 것 같다.

                                그 장면은 실제 사건이라기보다,
                                누군가가 모두에게 보여주고 싶었던 장면처럼 느껴진다.
                                """,
                        """
                                자국을 바라보고 있자 손끝이 이유 없이 떨린다.

                                발톱 자국이라기보다,
                                나무 표면을 억지로 긁어낸 흔적처럼 보인다.

                                손끝 안쪽에서 무언가가 갈라지는 듯한 감각이 스친다.
                                """,
                        """
                                성냥팔이 소녀는 긁힌 자국 가장자리의 작은 검은 얼룩을 먼저 본다.

                                불에 탄 흔적은 아니다.

                                하지만 누군가 무언가를 급히 문질러 지운 자국처럼 느껴진다.

                                흔적을 만들고, 다시 숨기려 한 손길.

                                그게 이상하게 낯설지 않다.
                                """,
                        ""),
                investigationEvidence("overturned-chair", 1, "뒤집힌 의자", "중앙 홀", "식탁 끝자리",
                        """
                                식탁 의자 하나만 크게 뒤로 밀려 있다.

                                누군가 급하게 일어난 흔적처럼 보인다.

                                의자 다리 한쪽에는 아주 희미하게 검은 재가 묻어 있다.

                                바닥에는 의자가 끌린 자국이 남아 있지만, 그 방향이 조금 이상하다.

                                누군가 앉아 있다가 놀라 일어난 것인지,
                                아니면 누군가 일부러 장면을 만들기 위해 밀어둔 것인지는 알 수 없다.
                                """,
                        """
                                의자가 뒤로 밀린 방향을 보는 순간,
                                눈밭 위로 등을 덮치던 무게가 떠오른다.

                                누군가에게 밀쳐진 것인지,
                                자신이 무언가를 밀쳐낸 것인지는 알 수 없다.

                                몸이 먼저 반응한다.
                                손끝이 차갑게 굳고, 숨이 짧아진다.
                                """,
                        """
                                몸싸움 흔적이라기보다,
                                누군가 일부러 남긴 장면처럼 느껴진다.

                                의자가 넘어져 있어야 한다는 사실을 누군가 뒤늦게 깨닫고,
                                그 장면을 맞춰놓은 것 같다.

                                앨리스는 의자 다리의 각도가 지나치게 정확하다고 느낀다.
                                """,
                        """
                                눈밭에서 무언가 쓰러지던 장면이 스친다.

                                붉은색이 아주 잠깐 보였던 것 같다.

                                그것이 사람인지, 망토인지,
                                자신이 보고 싶지 않았던 장면인지는 기억나지 않는다.

                                다만 의자가 밀려난 자리를 보고 있자,
                                자신이 방 안에서 더는 기다릴 수 없었던 순간이 떠오른다.
                                """,
                        """
                                의자 다리에 묻은 재 냄새가 이상하게 익숙하다.

                                아주 오래된 재가 아니다.

                                누군가가 작은 불을 켰고,
                                그 불이 꺼진 뒤 손끝에 남은 냄새와 비슷하다.
                                """,
                        """
                                아까 성냥팔이 소녀가 웃으며 했던 말이 떠오른다.

                                “태우면 없어질 줄 알았어.

                                그런데 냄새는 남더라.”
                                """),
                investigationEvidence("broken-teacup", 1, "깨진 찻잔", "중앙 홀", "벽난로 앞 바닥",
                        """
                                벽난로 앞 바닥에는 깨진 찻잔 조각이 흩어져 있다.

                                조각 대부분은 바깥쪽으로 크게 튀어 있고,
                                손잡이 부분만 유난히 멀리 떨어져 있다.

                                단순히 떨어뜨린 모양이라기보다,
                                누군가 급하게 밀쳐낸 것처럼 보인다.

                                찻잔 조각 아래에는 아주 희미한 그을음과
                                말라붙은 액체 자국이 함께 남아 있다.
                                """,
                        """
                                깨진 조각을 보는 순간,
                                눈 위로 무언가 흩어지던 장면이 떠오른다.

                                붉은 것.
                                하얀 것.
                                그리고 자신이 끝까지 바라보지 못했던 장면.

                                깨진 조각의 날카로운 끝을 보고 있자,
                                목덜미의 상처가 다시 욱신거린다.
                                """,
                        """
                                찻잔 조각 배치가 이상하다.

                                마치 누군가 일부러 깨진 방향을 바꿔놓은 것처럼 느껴진다.

                                앨리스는 자신도 모르게 찻잔 조각의 개수를 세기 시작한다.

                                하나, 둘, 셋.

                                개수를 세면 진정될 줄 알았지만,
                                오히려 이 장면이 아직 꿈인지 확인하고 싶어진다.
                                """,
                        """
                                깨지는 소리를 떠올리는 순간,
                                나무 관절 안쪽에서 무언가 금 가는 감각이 스친다.

                                그것이 찻잔이 깨진 소리였는지,
                                자기 안에서 무언가 부러진 소리였는지 구분하기 어렵다.

                                그는 소리보다 그 뒤에 찾아온 침묵이 더 불편하다.
                                """,
                        """
                                성냥팔이 소녀는 찻잔 아래의 희미한 탄 자국을 먼저 본다.

                                식탁 위로 뜨거운 액체가 튀던 감각이 떠오르고,
                                아주 잠깐 누군가 식탁을 강하게 내려치던 장면이 스친다.

                                하지만 그 장면이 실제 기억인지,
                                자신이 불안할 때마다 만들어내는 장난 같은 기억인지는 알 수 없다.
                                """,
                        ""),
                investigationEvidence("red-rabbit-painting", 1, "붉게 칠해진 토끼 그림", "중앙 홀", "벽난로 위 낡은 액자 아래",
                        """
                                토끼 그림의 목 부분만 붉게 덧칠되어 있다.

                                붉은 부분은 물감이라기보다 말라붙은 액체처럼 보인다.

                                칠해진 선은 일정하지 않고, 중간중간 흔들려 있다.

                                누군가 장난처럼 칠한 것이라기엔 선 끝에 망설임이 남아 있다.
                                """,
                        """
                                그림을 보는 순간, 눈 위에 번지던 피가 떠오른다.

                                하얀 눈 위에서 붉은색은 너무 선명했다.

                                자신은 그 색을 잊고 싶었지만,
                                그 색은 언제나 제일 먼저 떠오른다.
                                """,
                        """
                                붉은 선의 흔들림이 울면서 그린 그림처럼 보인다.

                                앨리스는 그 선이 목을 가르는 선이라기보다,
                                무언가를 멈추게 해달라고 매달리는 선처럼 느낀다.

                                그림 속 토끼는 웃고 있는 것 같기도 하고,
                                울고 있는 것 같기도 하다.
                                """,
                        """
                                그림을 보는 순간, 가슴 한쪽이 조여오는 느낌이 든다.

                                그 붉은 선은 피처럼 보이지만,
                                어쩐지 자신이 마지막으로 보고 싶지 않았던 얼굴을 가리기 위한 선처럼 느껴진다.

                                그는 그림을 오래 바라보지 못한다.
                                """,
                        """
                                붉은 선은 피처럼 보이지만,
                                동시에 누군가 지워버리고 싶었던 기억처럼 느껴진다.

                                무언가를 덧칠하면 아래에 있던 것이 사라질 줄 알았던 사람의 흔적.

                                하지만 덧칠된 색은 오히려 가장 숨기고 싶은 부분을 더 선명하게 만든다.
                                """,
                        ""),
                investigationEvidence("torn-record", 2, "찢어진 기록 일부", "토끼의 방", "책상 서랍 안",
                        """
                                토끼가 남긴 기록 일부가 거칠게 찢겨 있다.

                                “…기억이 다시 흔들리기 시작했다.”

                                라는 문장만 남아 있다.

                                이상하게도 기억과 관련된 부분만 사라져 있다.

                                종이의 찢긴 끝부분에는 손으로 급히 잡아뜯은 흔적과
                                아주 희미한 그을림이 함께 남아 있다.
                                """,
                        """
                                자신의 기억 역시 곳곳이 비어 있다는 사실이 떠오른다.

                                쓰러졌던 밤.
                                목덜미의 고통.
                                토끼 발자국.

                                중요한 장면일수록, 기억은 꼭 중간에서 끊겨 있다.
                                """,
                        """
                                앨리스는 이 기록이 단순히 찢긴 것이 아니라,
                                누군가 급하게 숨긴 흔적처럼 보인다고 느낀다.

                                기억과 관련된 부분만 사라진 것이 이상하다.

                                마치 누군가 기억 자체를 지우려 한 것이 아니라,
                                기억을 읽을 수 있는 문장만 골라 없앤 것 같다.
                                """,
                        """
                                문장을 읽는 순간, 누군가 계속 “잊어.”라고 속삭이는 느낌이 든다.

                                그 목소리는 밖에서 들리는 것 같지 않다.

                                오히려 자신의 안쪽,
                                나무 관절 사이 깊은 곳에서 울리는 것 같다.
                                """,
                        """
                                종이 끝부분에는 불에 그을린 흔적이 남아 있다.

                                완전히 태우려다 멈춘 것 같다.

                                성냥팔이 소녀는 그 흔적을 보고,
                                자신이 태우지 못하고 남겨둔 것들이 언제나 더 오래 남는다는 사실을 떠올린다.
                                """,
                        ""),
                investigationEvidence("wood-shavings", 2, "나무 가루", "복도", "시체가 놓여 있던 자리 근처",
                        """
                                미세한 나무 가루가 흩어져 있다.

                                피 냄새 대신 마른 목각 냄새가 희미하게 남아 있다.

                                누군가는 단순 먼지라고 말하지만,
                                오래된 인형 공방을 떠올린 사람도 있었다.

                                나무 가루는 시체가 놓였던 자리 근처에만 남아 있으며,
                                바닥을 따라 아주 짧게 끌린 흔적이 보인다.
                                """,
                        """
                                피 냄새보다 이상하게 나무 냄새가 먼저 느껴진다.

                                그 냄새는 숲의 나무 냄새와 다르다.
                                살아 있는 나무가 아니라, 깎이고 말라버린 나무의 냄새다.

                                빨간망토는 자신이 쫓던 것이 짐승이었는지,
                                아니면 전혀 다른 무언가였는지 잠시 헷갈린다.
                                """,
                        """
                                나무 가루를 보는 순간, 벽 안쪽에서 들리던 기계음이 떠오른다.

                                똑딱.

                                그 소리는 시계 소리처럼 들렸지만,
                                어쩌면 나무와 금속이 맞물리며 나는 소리였을지도 모른다.

                                현실이 아주 얇게 갈라지는 느낌이 든다.
                                """,
                        """
                                손끝에 묻은 나무 가루가 잘 떨어지지 않는 기분이 든다.

                                아무것도 만지지 않았는데도,
                                손가락 사이가 마른 나무 냄새로 가득 찬 것 같다.

                                가끔 자신의 몸에서는 아무도 만지지 않았는데도 나무 가루 냄새가 났다.

                                누군가 자신을 고치려 했던 것인지,
                                다시 묶어두려 했던 것인지는 알 수 없다.
                                """,
                        """
                                불에 타기 전 목재 냄새와 비슷하다.

                                성냥팔이 소녀는 그 냄새를 알고 있다.

                                아직 타지 않았지만,
                                불이 닿으면 너무 쉽게 사라질 것 같은 냄새.

                                그래서 더 불안하다.
                                """,
                        ""),
                investigationEvidence("stopped-watch-sketch", 2, "멈춘 시계 스케치", "앨리스의 방", "침대 맞은편 벽면",
                        """
                                멈춘 회중시계 그림이 반복해서 그려져 있다.

                                모든 시계 초침은 같은 위치를 가리키고 있다.

                                덧칠과 지운 흔적이 반복되어 있다.

                                처음 보면 낙서처럼 보이지만,
                                자세히 보면 모든 그림의 초침은 정확히 같은 위치에서 멈춰 있다.
                                """,
                        """
                                숫자를 보는 순간, 멈춰 있던 숲 공기가 떠오른다.

                                늑대들의 숨소리도, 눈 위의 발자국도,
                                자신의 심장 소리도 그 순간만은 모두 멈춰 있었던 것 같다.
                                """,
                        """
                                그 그림을 볼 때마다, 초침 소리가 들리는 것 같다.

                                하지만 무서운 것은 시계가 멈춘 순간이 아니다.

                                멈춘 뒤에도 들리는 소리다.

                                똑딱.
                                벽 안쪽에서, 아직 무언가가 돌아가고 있었다.
                                """,
                        """
                                심장이 이유 없이 빠르게 뛴다.

                                자신에게 심장이 있는지도 확신할 수 없는데,
                                그 숫자를 보는 순간 무언가가 안쪽에서 세게 두드리는 느낌이 든다.

                                그것은 두려움인지, 기억인지,
                                아니면 멈추지 못한 어떤 장치인지 알 수 없다.
                                """,
                        """
                                벽 일부는 손으로 여러 번 문질러 지운 흔적이 남아 있다.

                                성냥팔이 소녀는 그것이 불에 탄 흔적은 아니지만,
                                무언가를 지우려고 애쓴 흔적이라는 점에서 이상하게 익숙하다고 느낀다.
                                """,
                        ""),
                investigationEvidence("broken-thread", 2, "끊어진 실", "멈춘 회중시계 내부", "시계 내부",
                        """
                                가느다란 실 조각 하나가 발견되었다.

                                옷감용이라기엔 지나치게 질기고 균일한 실이다.

                                일부는 억지로 뜯긴 듯 끊어져 있고,
                                끝부분에는 아주 미세한 나무 가루가 묻어 있다.

                                시계 안에 왜 실이 들어 있었는지는 알 수 없다.

                                다만 이 실은 단순한 장식이 아니라,
                                무언가를 움직이거나 붙잡기 위해 쓰인 것처럼 보인다.
                                """,
                        """
                                누군가 자신을 붙잡고 있었다는 느낌이 스친다.

                                도망치던 밤,
                                자신이 숲에서 빠져나온 것이 아니라 누군가에게 이끌려 나온 것 같았던 감각이 떠오른다.

                                그것이 구원이었는지, 덫이었는지는 아직도 알 수 없다.
                                """,
                        """
                                단순 천 조각이 아니라, 무언가를 움직이기 위한 장치처럼 느껴진다.

                                앨리스는 실을 보는 순간, 이상한 나라의 무대 뒤편을 떠올린다.

                                앞에서는 모두가 웃고 있었지만,
                                뒤에서는 누군가 실을 당기고 있었다.
                                """,
                        """
                                실 끝을 보는 순간, 손가락 끝이 차갑게 굳는다.

                                실이 끊어지는 소리는 자유처럼 들릴 수도 있지만,
                                그에게는 버려지는 소리처럼 들렸다.

                                누군가 실을 당기면 움직이고, 놓으면 멈추는 것.

                                그 문장이 다시 떠오른다.
                                """,
                        """
                                오래된 목재 냄새와 금속 냄새가 함께 난다.

                                성냥팔이 소녀는 이 실이 불에 잘 타지 않을 것 같다고 생각한다.

                                무언가를 태워 없애려 해도,
                                이런 것은 끝까지 남을 것 같은 기분이 든다.
                                """,
                        ""),
                investigationEvidence("watch-inscription", 3, "시계 안쪽의 글자", "멈춘 회중시계 내부 금속판", "긁힌 금속판",
                        """
                                금속판 안쪽에 작은 글자가 긁혀 있다.

                                “멈춰.”

                                급하게 새긴 듯 글씨가 깊게 흔들려 있다.

                                글자 주변에는 손톱이나 날카로운 물건으로
                                몇 번이고 긁어낸 듯한 흔적이 남아 있다.

                                부탁처럼 보이기도 하고,
                                명령처럼 보이기도 하며,
                                절규처럼 보이기도 한다.
                                """,
                        """
                                누군가 울면서 무언가를 붙잡고 있던 모습이 떠오른다.

                                그 장면이 자신의 기억인지,
                                누군가의 감정이 묻어 들어온 것인지는 알 수 없다.

                                하지만 그 말은 공격의 말처럼 느껴지지 않는다.

                                오히려 더 이상 잃고 싶지 않은 사람이
                                마지막으로 붙잡은 말처럼 느껴진다.
                                """,
                        """
                                “멈춰”라는 글씨는 부탁이라기보다 절규처럼 느껴진다.

                                앨리스는 시계가 멈춘 것이 아니라,
                                누군가 멈추길 바랐던 장면만 남은 것 같다고 생각한다.

                                시간은 멈추지 않았고,
                                기억만 그 순간에 걸려 있는 것 같다.
                                """,
                        """
                                “멈춰”라는 글자를 보는 순간,
                                손끝이 자기 것이 아닌 것처럼 떨린다.

                                누군가 실을 당기던 힘이,
                                아직 손가락 끝에 남아 있는 것 같다.

                                그 말은 토끼에게 한 말이었는지,
                                자기 자신에게 한 말이었는지 알 수 없다.
                                """,
                        """
                                글씨 끝부분에는 손톱으로 긁어낸 듯한 거친 흔적이 남아 있다.

                                성냥팔이 소녀는 그 흔적을 보고
                                무언가를 지우려는 손보다, 무언가를 붙잡으려는 손을 떠올린다.

                                불로 태워 지우는 것과는 다른 흔적이다.
                                그래서 더 무섭다.
                                """,
                        ""),
                investigationEvidence("repeated-sentence", 3, "반복된 문장", "숨겨진 작업실", "불탄 종이 더미",
                        """
                                구겨진 종이 여러 장에 같은 문장이 반복되어 적혀 있다.

                                “기억하지 마.”
                                “기억하지 마.”
                                “기억하지 마.”

                                마지막 줄로 갈수록 글씨가 무너져 있고,
                                종이 끝에는 검게 그을린 자국이 남아 있다.

                                급하게 태우려 한 흔적이 있지만,
                                이상하게도 문장만은 끝까지 남아 있다.
                                """,
                        """
                                자신도 무언가를 잊고 있다는 기분이 든다.

                                기억이 끊긴 밤마다,
                                자신은 늘 같은 질문 앞에 돌아왔다.

                                내가 했을까.
                                또 기억을 잃은 사이에,
                                내가 정말 괴물이 되어버린 걸까.

                                이 문장은 자신에게도 향하는 말처럼 느껴진다.
                                """,
                        """
                                누군가 기억을 억지로 붙잡다가 망가진 것처럼 보인다.

                                앨리스는 글씨가 반복될수록
                                그 문장이 명령이 아니라 주문처럼 느껴진다.

                                잊으려 할수록,
                                오히려 더 선명해지는 종류의 기억이 있다.
                                """,
                        """
                                문장을 보는 순간, 숨이 막히는 느낌이 든다.

                                기억하지 말라는 말은 잊고 싶다는 말과 다르다.

                                그건 기억하고 있다는 사실을 알고 있는 사람이
                                스스로에게 내리는 명령처럼 보인다.

                                그는 종이를 오래 바라보지 못한다.
                                """,
                        """
                                누군가 급하게 태우려다 멈춘 흔적이다.

                                성냥팔이 소녀는 그을린 종이 끝을 바라본다.

                                태우면 없어질 줄 알았다.

                                하지만 재는 늘 정직했다.

                                무엇을 태웠는지 말하지는 않았지만,
                                무언가를 태웠다는 사실만큼은 끝까지 숨기지 않았다.
                                """,
                        ""),
                investigationEvidence("cracked-doll-eye", 3, "금 간 인형 눈", "숨겨진 작업실", "인형 진열장 맨 안쪽",
                        """
                                오래된 목각 인형의 눈동자에 깊은 금이 가 있다.

                                깨진 틈 사이에는 검은 먼지와 나무 가루가 끼어 있다.

                                이상하게도, 누구도 그 인형을 오래 바라보지 못한다.

                                인형의 눈은 움직이지 않지만,
                                고개를 돌린 뒤에도 마치 누군가 계속 자신을 바라보고 있는 듯한 기분이 남는다.

                                금 간 틈 사이에는 아주 오래된 실 조각 하나가 끼어 있다.
                                """,
                        """
                                인형 눈을 바라보는 순간,
                                숲속에서 자신을 바라보던 거대한 시선이 떠오른다.

                                하지만 그 눈이 늑대의 것이었는지,
                                다른 누군가의 것이었는지는 기억나지 않는다.

                                그 시선에는 굶주림보다 오래된 슬픔이 섞여 있었던 것 같다.
                                """,
                        """
                                금 간 눈동자가 울고 있는 것처럼 보인다.

                                그리고 아주 잠깐,
                                인형 눈이 자신을 따라 움직인 것 같은 착각이 든다.

                                앨리스는 그것이 환각인지,
                                아니면 자신만 본 진실의 틈인지 확신하지 못한다.
                                """,
                        """
                                인형 얼굴을 보는 순간, 숨이 막히는 느낌이 든다.

                                고개를 돌리고 싶지만, 이상하게도 눈을 떼기 어렵다.

                                마치 오래전부터 저 눈이 자신을 알고 있었던 것처럼 느껴진다.

                                금 간 눈동자는 자신이 흉내 내던 얼굴이 아니라,
                                끝내 숨기지 못한 얼굴처럼 보인다.
                                """,
                        """
                                인형 안쪽 나무 냄새에서는 희미하게 탄 흔적이 섞여 있다.

                                그리고 금 간 틈 사이에,
                                아주 오래된 실 조각 하나가 끼어 있는 것을 발견한다.

                                성냥팔이 소녀는 그것을 보며 생각한다.

                                태워도 남는 것이 있고,
                                끊어져도 남는 것이 있다.
                                """,
                        "")
        );
    }

    private InvestigationEvidence investigationEvidence(String id,
                                                        int act,
                                                        String title,
                                                        String locationName,
                                                        String detailLocation,
                                                        String commonDescription,
                                                        String redInterpretation,
                                                        String aliceInterpretation,
                                                        String pinoInterpretation,
                                                        String matchInterpretation,
                                                        String recalledLine) {
        return new InvestigationEvidence(
                id,
                act,
                title,
                locationName,
                detailLocation,
                commonDescription.strip(),
                Map.of(
                        "RED", redInterpretation.strip(),
                        "ALICE", aliceInterpretation.strip(),
                        "PINO", pinoInterpretation.strip(),
                        "MATCH", matchInterpretation.strip()
                ),
                recalledLine == null ? "" : recalledLine.strip()
        );
    }

    private String slug(String text) {
        return text.toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9가-힣-]", "");
    }

    public List<PersonalHint> sharedEvidenceHints() {
        return hints.values().stream()
                .filter(hint -> hint.type() == HintType.EVIDENCE)
                .filter(hint -> sharedHintIds.contains(hint.id()))
                .collect(Collectors.toList());
    }

    public void revealEnding() {
        state.setPhase(GamePhase.ENDING);
        state.setTimerRunning(false);
        state.setTimerEndsAt(null);
        endingRevealed = true;
    }

    public boolean isEndingRevealed() {
        return endingRevealed;
    }

    public EndingText endingText() {
        return new EndingText(
                "토끼가 남긴 마지막 시간",
                """
                        회중시계는 처음부터 사건의 시간이 아니었습니다.

                        그것은 토끼가 수없이 되돌아간 시간의 흔적이었습니다.

                        토끼는 미래를 볼 수 있었습니다. 그리고 그 미래 속에서, RED는 자신의 몸을 괴물이라 믿은 채 파멸했고, ALICE는 아무도 믿어주지 않는 진실 속에서 길을 잃었고, MATCH는 불길 속에서 끝내 혼자가 되었고, PINO는 자신 안의 악의에 삼켜져 사람들의 기억을 부수는 존재가 되었습니다.

                        토끼는 그 죽음들을 보았습니다.

                        그래서 그는 시간을 거슬러 숲으로 들어갔습니다. 눈보라 속에서 RED를 이끌었고, 길을 잃은 ALICE에게 말을 걸었고, 불길 앞의 MATCH에게 손을 내밀었고, 실에 묶인 PINO에게 처음으로 선택이라는 말을 주었습니다.

                        그는 단순한 토끼가 아니었습니다.

                        그의 진짜 이름은 제페토.

                        한때 그는 사람의 마음과 기억을 조종하려 했던 공학자였습니다. PINO는 그의 악의에서 만들어진 존재였습니다. 그러나 PINO가 인간을 해치지 못하고 고통스러워하는 모습을 보며, 제페토는 자신이 만든 아이 역시 또 하나의 피해자였다는 것을 깨달았습니다.

                        그래서 제페토는 모든 것을 바로잡으려 했습니다.

                        네 사람을 살리기 위해, 자신의 죄를 끝내기 위해, 그는 토끼의 모습으로 시간을 반복했습니다. 하지만 미래를 바꾸는 유일한 길은 네 사람이 서로 만나고, 서로의 상처와 거짓과 기억을 마주하게 만드는 것이었습니다.

                        그리고 마지막에는, 자신이 죽어야 했습니다.

                        PINO는 토끼를 미워해서 죽인 것이 아니었습니다.

                        토끼가 죽기 전, 그는 PINO에게 진실을 말했습니다. 네가 괴물로 만들어졌다는 것. 네 안의 능력은 누군가를 지배하기 위해 심어진 악의였다는 것. 하지만 그것만으로 네가 악한 존재가 되는 것은 아니라는 것.

                        PINO는 그 말을 견디지 못했습니다.

                        자신이 누군가의 불행을 위해 만들어졌다는 사실이 모두에게 드러나면, 처음으로 자신을 사람처럼 대해준 이들마저 자신을 버릴 거라고 믿었습니다. 그래서 그는 토끼의 기억을 지우려 했습니다. 정확히는, 토끼가 알아낸 자신의 정체와 제페토의 얼굴을 지우려 했습니다.

                        하지만 토끼의 회중시계는 이미 너무 많은 시간을 품고 있었습니다. PINO의 기억 조작은 시계 속 반복된 시간과 충돌했고, 지워야 할 기억 대신 토끼가 붙잡고 있던 마지막 시간이 찢어졌습니다.

                        그래서 토끼는 죽었습니다.

                        살의보다 두려움에 가까운 선택이었고, 공격보다 도망에 가까운 손짓이었습니다. 그러나 그 선택이 이 밤의 죽음을 만들었습니다.

                        PINO가 지우려 했던 기억은 살인의 기억이 아니라, 자신을 구하려 했던 존재의 얼굴이었습니다.

                        토끼는 처음부터 알고 있었습니다.

                        자신이 이 밤의 끝에서 죽게 된다는 것을.

                        그래도 그는 멈추지 않았습니다. 누군가를 만든 죄가 있다면, 마지막에는 누군가를 살리는 선택도 할 수 있다고 믿었기 때문입니다.

                        이제 남은 것은 범인의 이름만이 아닙니다.

                        당신들이 왜 살아남았는지.

                        왜 모두 토끼를 따라 숲을 빠져나왔는지.

                        그리고 서로를 의심하던 이 밤이, 사실은 서로를 살리기 위해 준비된 마지막 무대였다는 사실입니다.
                        """
        );
    }

    public PrologueText prologueFor(Player player) {
        StoryContent external = storyContents.get(player.code());
        if (external != null) {
            return new PrologueText(external.prologueTitle(), external.prologueParagraphs(), "오두막 안으로 들어간다");
        }
        return switch (player.code()) {
            case "PINO" -> new PrologueText(
                    "사람이 되고 싶다는 말",
                    List.of(
                            "눈이 내리던 밤, 토끼는 당신 앞에 서서 말했다. \"사람이 되고 싶다면, 나를 따라와.\"",
                            "그 말은 명령이 아니었다. 실을 잡아당기는 손도, 나무 관절을 움직이는 기계음도 없었다. 당신은 처음으로 누군가의 의지가 아니라 자신의 발로 숲길을 걸었다.",
                            "토끼가 데려간 곳은 거대한 저택이 아니라 눈 속에 파묻힌 작은 오두막이었다. 창문 너머에는 촛불이 흔들렸고, 안에는 이미 다른 존재들의 기척이 있었다.",
                            "토끼는 당신에게 작은 방을 내주었다. \"여기서 기다려. 마지막으로 한 소녀를 데려와야 해.\" 그는 그렇게 말하고 다시 눈보라 속으로 사라졌다.",
                            "당신은 방에서 기다렸다. 벽 너머에서는 누군가 걷는 소리가 들렸고, 멀리서는 문이 열리고 닫히는 소리가 났다. 그러다 창밖, 오두막의 정반대편 눈밭에 쓰러진 붉은 그림자를 보았다.",
                            "그 순간, 숲 어딘가에서 누군가 비명을 삼킨 듯한 침묵이 내려앉았다."
                    ),
                    "오두막 안으로 들어간다"
            );
            case "ALICE" -> new PrologueText(
                    "원래 세계로 돌아갈 수 있다는 말",
                    List.of(
                            "눈이 내리던 밤, 토끼는 당신에게 말했다. \"돌아가고 싶지 않아? 네가 원래 있던 세계로.\"",
                            "원래 세계. 그 말은 너무 낯설어서 오히려 믿고 싶어졌다. 당신은 토끼의 작은 발자국을 따라 숲속으로 들어갔다. 나무들은 길을 막는 대신, 당신이 지나가자 조금씩 비켜서는 것처럼 보였다.",
                            "도착한 곳은 오래된 작은 오두막이었다. 이상한 나라의 문도, 궁전도 아니었다. 다만 안쪽에서 희미한 촛불과 낯선 숨소리들이 새어 나왔다.",
                            "토끼는 말했다. \"각자의 방에서 기다려. 마지막으로 한 소녀를 데려와야 해.\" 그리고 당신이 대답하기도 전에, 그는 눈발 사이로 사라졌다.",
                            "하지만 당신은 기다리지 못했다. 벽 너머에서 초침 소리 같은 것이 들렸고, 복도 끝에서 누군가 속삭이는 것 같았다. 당신은 조용히 방을 나와 오두막 안을 돌아다녔다.",
                            "그때 창밖에서 붉은 무언가가 눈 위에 쓰러진 것을 보았다. 그리고 그보다 더 멀리, 토끼가 돌아오지 않는다는 사실을 깨달았다."
                    ),
                    "오두막 안으로 들어간다"
            );
            case "MATCH" -> new PrologueText(
                    "춥지 않은 곳에 살 수 있다는 말",
                    List.of(
                            "눈이 내리던 밤, 토끼는 당신 앞에 멈춰 섰다. \"춥지 않은 곳이 있어. 그곳에서는 더는 손을 녹이려고 불을 켜지 않아도 돼.\"",
                            "당신은 웃어넘기려 했지만, 손끝이 너무 차가웠다. 그래서 따라갔다. 눈 속의 작은 발자국을 밟으며, 불빛 하나 없는 숲을 지나갔다.",
                            "토끼가 데려간 곳은 눈 속에 숨은 오두막이었다. 안에는 벽난로가 있었고, 방마다 촛불이 켜져 있었다. 이상하게도 혼자가 아니라는 느낌이 먼저 들었다.",
                            "토끼는 당신에게 방을 알려주고 말했다. \"기다려. 마지막으로 한 소녀를 데려와야 해.\" 그 목소리는 다정했지만, 어딘가 서둘러 있었다.",
                            "당신은 방 안에 오래 있지 못했다. 따뜻한 공기가 낯설어서, 오히려 숨이 막혔다. 결국 외투를 걸치고 잠깐 산책을 나섰다.",
                            "눈길을 따라 걷던 당신은 오두막 뒤편에서 토끼를 발견했다. 그는 차가운 눈 위에 쓰러져 있었고, 회중시계는 11시 47분에서 멈춰 있었다."
                    ),
                    "오두막 안으로 돌아간다"
            );
            case "RED" -> new PrologueText(
                    "마지막으로 데려온 소녀",
                    List.of(
                            "눈이 내리던 밤, 당신은 토끼를 따라 숲으로 들어갔다. 왜 따라갔는지는 분명하지 않다. 다만 그가 자꾸 뒤돌아보며 말했다. \"조금만 더. 모두 기다리고 있어.\"",
                            "숲은 너무 조용했다. 늑대의 냄새인지, 피 냄새인지, 젖은 나무 냄새인지 알 수 없는 것이 목을 조였다. 당신은 토끼의 회중시계가 흔들리는 소리만 따라 걸었다.",
                            "멀리 작은 오두막의 불빛이 보였을 때, 갑자기 기억이 끊겼다.",
                            "다음 장면에서 당신은 눈 위에 쓰러져 있었다. 오두막의 정문이 아니라, 정반대편 숲가였다. 손은 차갑고, 머리는 깨질 듯 아팠고, 망토 끝에는 눈이 얼어붙어 있었다.",
                            "창문 너머 어딘가에서 누군가 당신을 본 것 같았다. 인형처럼 움직이지 않는 그림자. 그리고 조금 뒤, 누군가가 토끼를 발견했다는 소리가 오두막 안을 갈랐다.",
                            "당신은 토끼가 마지막으로 데려오려던 소녀였다. 하지만 그가 당신을 끝까지 데려왔는지, 아니면 당신이 그에게서 도망쳤는지는 기억나지 않는다."
                    ),
                    "눈을 뜬다"
            );
            default -> new PrologueText(
                    "눈 내리는 오두막",
                    List.of(
                            "눈 내리는 숲 깊은 곳, 토끼는 네 사람을 작은 오두막으로 이끌었습니다.",
                            "각자는 다른 약속을 들었고, 다른 이유로 그 발자국을 따라왔습니다.",
                            "그리고 토끼는 마지막으로 한 소녀를 데려오겠다는 말을 남긴 채, 다시 눈 속으로 사라졌습니다."
                    ),
                    "오두막 안으로 들어간다"
            );
        };
    }

    public ShareResult shareMyHint(Player player, String hintId) {
        PersonalHint hint = hints.get(hintId);
        if (hint == null || !hint.playerCode().equals(player.code()) || !visibleHintsFor(player).contains(hint)) {
            return new ShareResult(false, "공개할 수 없는 힌트입니다.");
        }
        if (!sharedHintIds.add(hint.id())) {
            return new ShareResult(true, "이미 공개된 힌트입니다.");
        }
        publicRecords.add(new PublicRecord(
                "hint-" + hint.id(),
                player.code(),
                player.name(),
                hint.type().getLabel(),
                hint.title(),
                hint.body(),
                hint.roundLabel(),
                Instant.now()
        ));
        return new ShareResult(true, "선택한 힌트를 공개된 기록에 올렸습니다.");
    }

    public List<PlayerDisclosureStatus> playerDisclosureStatuses() {
        return playableCharacters().stream()
                .map(player -> new PlayerDisclosureStatus(
                        player,
                        isPersonalStoryRevealedFor(player),
                        hintStatusFor(player, GamePhase.FIRST_HINT),
                        hintStatusFor(player, GamePhase.SECOND_HINT),
                        evidenceStatusesFor(player),
                        hasSharedHint(player)
                ))
                .collect(Collectors.toList());
    }

    public List<PersonalHint> visibleHintsFor(Player player) {
        return hints.values().stream()
                .filter(hint -> hint.playerCode().equals(player.code()))
                .filter(hint -> revealedHintIds.contains(hint.id()))
                .collect(Collectors.toList());
    }

    public List<PersonalHint> stageHintsFor(Player viewer) {
        return hints.values().stream()
                .filter(hint -> revealedHintIds.contains(hint.id()) || sharedHintIds.contains(hint.id()))
                .collect(Collectors.toList());
    }

    public boolean canReadHint(Player viewer, PersonalHint hint) {
        return hint.playerCode().equals(viewer.code()) || sharedHintIds.contains(hint.id());
    }

    public List<PublicInfoStatus> publicInfoStatusesFor(Player viewer) {
        return playableCharacters().stream()
                .map(player -> new PublicInfoStatus(
                        player,
                        player.code().equals(viewer.code()),
                        hasSharedHint(player)
                ))
                .collect(Collectors.toList());
    }

    public List<PublicRecord> publicRecords() {
        return List.copyOf(publicRecords);
    }

    public List<String> visibleHintIdsFor(Player player) {
        return visibleHintsFor(player).stream()
                .map(PersonalHint::id)
                .collect(Collectors.toList());
    }

    public boolean isHintShared(String hintId) {
        return sharedHintIds.contains(hintId);
    }

    public List<PlayerStatus> playerStatuses() {
        return enteredPlayers.entrySet().stream()
                .map(entry -> new PlayerStatus(players.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    public List<Player> playableCharacters() {
        return players.values().stream()
                .filter(player -> !player.code().equals("ADMIN"))
                .collect(Collectors.toList());
    }

    public VoteResult vote(Player voter, String suspectCode) {
        if (state.getPhase() != GamePhase.VOTE) {
            return new VoteResult(false, "지금은 투표 시간이 아닙니다.");
        }
        Player suspect = players.get(suspectCode == null ? "" : suspectCode.trim().toUpperCase());
        if (suspect == null || suspect.code().equals("ADMIN")) {
            return new VoteResult(false, "선택할 수 없는 인물입니다.");
        }
        votes.put(voter.code(), suspect.code());
        return new VoteResult(true, suspect.name() + "에게 투표했습니다.");
    }

    public List<VoteStatus> voteStatuses() {
        return votes.entrySet().stream()
                .map(entry -> new VoteStatus(players.get(entry.getKey()), players.get(entry.getValue())))
                .collect(Collectors.toList());
    }

    private boolean isHintRevealable(PersonalHint hint) {
        return releasedHintPhases.contains(hint.phase()) || revealedHintIds.contains(hint.id());
    }

    private HintStatus hintStatusFor(Player player, GamePhase phase) {
        PersonalHint hint = hints.values().stream()
                .filter(item -> item.playerCode().equals(player.code()))
                .filter(item -> item.type() == HintType.EVIDENCE)
                .filter(item -> item.phase() == phase)
                .findFirst()
                .orElse(null);
        if (hint == null) {
            return new HintStatus(null, null, null, null, null, false, false);
        }
        return new HintStatus(hint.id(), hint.type().getLabel(), hint.roundLabel(), hint.title(), hint.body(), revealedHintIds.contains(hint.id()), isHintRevealable(hint));
    }

    private List<HintStatus> evidenceStatusesFor(Player player) {
        return hints.values().stream()
                .filter(item -> item.playerCode().equals(player.code()))
                .filter(item -> item.type() == HintType.EVIDENCE)
                .map(hint -> new HintStatus(
                        hint.id(),
                        hint.type().getLabel(),
                        hint.roundLabel(),
                        hint.title(),
                        hint.body(),
                        revealedHintIds.contains(hint.id()),
                        isHintRevealable(hint)
                ))
                .collect(Collectors.toList());
    }

    public GlobalDisclosureStatus globalDisclosureStatus() {
        long releasedHints = hints.values().stream()
                .filter(hint -> revealedHintIds.contains(hint.id()))
                .count();
        return new GlobalDisclosureStatus(
                unlockedPersonalStoryCodes.size(),
                playableCharacters().size(),
                releasedHintRoundCount(),
                hintReleasePhases().size(),
                releasedHints,
                hints.size()
        );
    }

    private boolean hasSharedHint(Player player) {
        return hints.values().stream()
                .filter(hint -> hint.playerCode().equals(player.code()))
                .anyMatch(hint -> sharedHintIds.contains(hint.id()));
    }

    private void syncTimer() {
        if (!state.isTimerRunning() || state.getTimerEndsAt() == null) {
            return;
        }
        long remaining = Duration.between(Instant.now(), state.getTimerEndsAt()).toSeconds();
        if (remaining <= 0) {
            state.setTimerSeconds(0);
            state.setTimerRunning(false);
            state.setTimerEndsAt(null);
            return;
        }
        state.setTimerSeconds((int) remaining);
    }

    public record HintStatus(String hintId, String typeLabel, String roundLabel, String title, String body, boolean revealed, boolean revealable) {
    }

    public record HintRoundStatus(GamePhase phase, int round, boolean released, long itemCount) {
    }

    public record PlayerDisclosureStatus(
            Player player,
            boolean storyRevealed,
            HintStatus firstHint,
            HintStatus secondHint,
            List<HintStatus> evidenceHints,
            boolean hintsShared
    ) {
    }

    public record PublicInfoStatus(Player player, boolean self, boolean hintsShared) {
    }

    public record PublicRecord(
            String id,
            String playerCode,
            String playerName,
            String type,
            String title,
            String body,
            String phaseLabel,
            Instant sharedAt
    ) {
    }

    public record EvidenceItem(
            String id,
            String revealRound,
            String category,
            String name,
            String shortDescription,
            String detail,
            List<String> keywords,
            String visual
    ) {
    }

    public record EvidenceStatus(EvidenceItem evidence, boolean revealed) {
    }

    public record InvestigationScene(
            String id,
            String title,
            String summary,
            List<String> clues,
            List<String> interpretations,
            String emotionalQuestion
    ) {
    }

    public record InvestigationSceneView(
            String id,
            String title,
            String summary,
            List<String> clues,
            List<String> personalInterpretations
    ) {
    }

    public record InvestigationActStatus(int act, String label, boolean open, long itemCount) {
    }

    public record InvestigationEvidence(
            String id,
            int act,
            String title,
            String locationName,
            String detailLocation,
            String commonDescription,
            Map<String, String> interpretations,
            String recalledLine
    ) {
    }

    public record InvestigationEvidenceView(
            String id,
            int act,
            String title,
            String locationName,
            String detailLocation,
            String commonDescription,
            String personalInterpretation,
            String recalledLine
    ) {
    }

    public record InvestigationLocationView(
            int act,
            String id,
            String title,
            String summary,
            List<InvestigationEvidenceView> evidenceItems
    ) {
    }

    public enum EvidenceDiscoveryStatus {
        PRIVATE_FOUND,
        PUBLIC
    }

    public record InvestigationLocation(
            String id,
            int act,
            String name,
            String description
    ) {
    }

    public record LocationOccupancy(
            String locationId,
            String playerCode,
            Instant occupiedAt
    ) {
    }

    public record PlayerEvidenceState(
            String playerCode,
            String evidenceId,
            EvidenceDiscoveryStatus status,
            Instant discoveredAt,
            Instant publishedAt
    ) {
    }

    public record PublishedEvidence(
            String evidenceId,
            String publishedByPlayerCode,
            String sourceLocationId,
            Instant publishedAt
    ) {
    }

    public record InvestigationEvidenceSummary(
            String id,
            String title,
            String detailLocation,
            boolean publicItem,
            boolean privateFound
    ) {
    }

    public record InvestigationLocationStatus(
            String id,
            int act,
            String name,
            String description,
            boolean open,
            boolean occupied,
            boolean occupiedBySelf,
            boolean canEnter,
            String occupiedByName,
            Instant occupiedAt,
            List<InvestigationEvidenceSummary> evidenceItems
    ) {
    }

    public record EvidenceDetailView(
            String id,
            String title,
            String locationName,
            String detailLocation,
            String commonDescription,
            String personalInterpretation,
            String recalledLine,
            boolean publicItem,
            String publishedByName
    ) {
    }

    public record EvidenceDetailResult(boolean success, String message, EvidenceDetailView evidence) {
    }

    public record InvestigationActionResult(boolean success, String message) {
    }

    public record PlayerEvidenceView(
            String id,
            String title,
            String locationName,
            String detailLocation,
            String locationId,
            String ownerName,
            Instant foundAt,
            EvidenceDiscoveryStatus status
    ) {
    }

    public record AdminLocationStatus(
            InvestigationLocation location,
            boolean open,
            Player occupant,
            Instant occupiedAt
    ) {
    }

    public record AdminPrivateEvidenceStatus(Player player, List<PlayerEvidenceView> evidenceItems) {
    }

    private static class InvestigationLocationBuilder {
        private final int act;
        private final String id;
        private final String title;
        private final String summary;
        private final List<InvestigationEvidenceView> evidenceItems = new java.util.ArrayList<>();

        InvestigationLocationBuilder(int act, String id, String title, String summary) {
            this.act = act;
            this.id = id;
            this.title = title;
            this.summary = summary;
        }

        void add(InvestigationEvidenceView evidence) {
            evidenceItems.add(evidence);
        }

        InvestigationLocationView build() {
            return new InvestigationLocationView(act, id, title, summary, List.copyOf(evidenceItems));
        }
    }

    public record GlobalDisclosureStatus(long storyUnlockedCount, long storyTotalCount, long firstHintsRevealed, long firstHintsTotal, long secondHintsRevealed, long secondHintsTotal) {
        public boolean storyUnlocked() {
            return storyTotalCount > 0 && storyUnlockedCount >= storyTotalCount;
        }
    }

    public record EndingText(String title, String body) {
    }

    public record PrologueText(String title, List<String> paragraphs, String buttonText) {
    }

    private record StoryContent(String prologueTitle, List<String> prologueParagraphs, String personalStory, String secretAndActingHint) {
    }

    public record ShareResult(boolean success, String message) {
    }

    public record PlayerStatus(Player player, Instant enteredAt) {
    }

    public record VoteResult(boolean success, String message) {
    }

    public record VoteStatus(Player voter, Player suspect) {
    }
}
