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
    private final Map<String, StoryContent> storyContents = new LinkedHashMap<>();
    private final Set<String> revealedHintIds = new LinkedHashSet<>();
    private final Set<String> sharedHintIds = new LinkedHashSet<>();
    private final Set<String> unlockedPersonalStoryCodes = new LinkedHashSet<>();
    private final Set<GamePhase> releasedHintPhases = new LinkedHashSet<>();
    private final List<PublicRecord> publicRecords = new java.util.ArrayList<>();
    private final Map<String, Instant> enteredPlayers = new LinkedHashMap<>();
    private final Map<String, String> votes = new LinkedHashMap<>();
    private final Map<String, PublishedPrivateEvidence> publishedPrivateEvidence = new LinkedHashMap<>();
    private final Set<GamePhase> completedDiscussions = new LinkedHashSet<>();
    private int publicEvidenceReleaseLevel;
    private int privateEvidenceReleaseLevel;
    private boolean endingRevealed;
    private boolean voteClosed;
    private int chapterCueSequence;
    private ChapterCue currentChapterCue;

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
                """
                        연기 힌트

                        사람들과 조금 거리를 두고,
                        항상 불안한 듯 조심스럽게 행동해주세요.

                        자신의 기억이 완전하지 않아서
                        무슨 일이 있었는지 확신하지 못하는 느낌으로 말하면 됩니다.

                        누군가 자신을 의심하면 당황하고,
                        자기 자신도 조금 무서워하는 모습으로 연기해주세요.
                        """, "#a91f2f"));
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
                """
                        연기 힌트

                        말할 때 가끔 자신의 기억이 맞는지 확신하지 못하는 모습을 보여주세요.

                        분명히 무언가를 봤다고 생각하지만,
                        그게 현실이었는지 환각이었는지는 잘 모릅니다.

                        토끼 이야기가 나오면 다른 사람들보다 조금 더 예민하게 반응하고,
                        토끼를 믿고 싶어 하는 모습을 보여주세요.

                        누군가 당신의 말을 믿지 않아도
                        강하게 화내기보다는 스스로도 흔들리는 듯 말하면 됩니다.

                        가끔 이렇게 말해도 좋습니다.

                        “분명히 봤어. 그런데… 정말이었을까?”

                        “토끼였던 것 같아. 아니, 사람이었나?”

                        “내 기억이 틀린 건지, 누가 바꾼 건지 모르겠어.”
                        """, "#4f86a8"));
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
                """
                        연기 힌트

                        사람처럼 자연스럽게 행동하려 하지만,
                        감정을 표현할 때 조금 어색한 모습을 보여주세요.

                        평소에는 차분하게 말하고,
                        질문을 받으면 잠시 생각한 뒤 대답하면 됩니다.

                        토끼 이야기가 나오면 슬픈 척하려 하지만,
                        토끼가 다른 아이들도 구해줬다는 말을 들으면
                        조금 서운하거나 불편한 기색을 보여주세요.

                        누군가 자신을 의심하면 바로 화내기보다
                        조용히 변명하면서 끝까지 부정해주세요.

                        계속 몰아붙이면
                        침착함이 조금씩 무너지고,
                        버려지고 싶지 않았다는 감정이 드러나도 좋습니다.
                        """, "#b9863a"));
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
                """
                        연기 힌트

                        분위기가 무거워지면
                        농담을 하거나 딴소리를 하면서 넘기려고 해주세요.

                        평소에는 밝고 장난스럽게 행동하지만,
                        조용해지는 순간에는 조금 불안해 보이면 좋습니다.

                        불, 성냥, 검은 재 이야기가 나오면
                        표정이 잠깐 굳거나 당황한 모습을 보여주세요.

                        누군가 화재 이야기를 꺼내면
                        처음에는 웃어넘기려고 하지만,
                        계속 질문받으면 조금씩 흔들리는 느낌으로 연기하면 됩니다.
                        """, "#d16b42"));
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

    }

    private void addTestimony(String id, String playerCode, String title, String body) {
        GamePhase phase = phaseForHintId(id);
        hints.put(id, new PersonalHint(id, playerCode, HintType.TESTIMONY, roundLabelFor(phase, "증언"), phase,
                displayNames(title), displayNames(body)));
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
        String secretAndActingHint = actingBlock.trim();
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
        state.setPhase(GamePhase.PRIVATE_STORY);
        triggerChapterCue("PROLOGUE_END", "프롤로그 종료", 4200,
                "하지만 이번에는,",
                "토끼가 돌아오지 못했다.");
    }

    public void unlockPersonalStory(String playerCode) {
        Player player = players.get(playerCode == null ? "" : playerCode.trim().toUpperCase());
        if (player != null && !player.code().equals("ADMIN")) {
            unlockedPersonalStoryCodes.add(player.code());
        }
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

    public List<SimpleEvidence> publicEvidenceBoard() {
        return simpleEvidenceItems().stream()
                .filter(evidence -> evidence.type() == SimpleEvidenceType.PUBLIC)
                .filter(evidence -> evidence.revealOrder() <= publicEvidenceReleaseLevel)
                .sorted(java.util.Comparator.comparingInt(SimpleEvidence::displayOrder))
                .collect(Collectors.toList());
    }

    public List<SimpleEvidence> privateEvidenceForPlayer(Player player) {
        if (player == null || player.code().equals("ADMIN")) {
            return List.of();
        }
        return simpleEvidenceItems().stream()
                .filter(evidence -> evidence.type() == SimpleEvidenceType.PRIVATE)
                .filter(evidence -> player.code().equals(evidence.characterCode()))
                .filter(evidence -> evidence.revealOrder() <= privateEvidenceReleaseLevel)
                .sorted(java.util.Comparator.comparingInt(SimpleEvidence::displayOrder))
                .collect(Collectors.toList());
    }

    public List<PublishedPrivateEvidenceView> publishedPrivateEvidenceBoard() {
        return publishedPrivateEvidence.values().stream()
                .map(published -> simpleEvidenceById(published.evidenceId())
                        .map(evidence -> new PublishedPrivateEvidenceView(
                                evidence.id(),
                                evidence.title(),
                                evidence.locationText(),
                                evidence.publicDescription(),
                                published.publishedByCharacterCode(),
                                playerByCode(published.publishedByCharacterCode()).map(Player::name).orElse(published.publishedByCharacterCode()),
                                published.publishedAt()
                        )))
                .flatMap(Optional::stream)
                .sorted(java.util.Comparator.comparing(PublishedPrivateEvidenceView::publishedAt))
                .collect(Collectors.toList());
    }

    public void releasePublicEvidence(int revealOrder) {
        int order = clampEvidenceRevealOrder(revealOrder);
        if (order == publicEvidenceReleaseLevel + 1) {
            publicEvidenceReleaseLevel = order;
            state.setPhase(order == 1 ? GamePhase.FIRST_HINT : GamePhase.SECOND_HINT);
            if (order == 1) {
                triggerChapterCue("ACT1_START", "1막 시작", 3800,
                        "눈은 계속 내리고 있었다.",
                        "아무 일도 없었다는 듯이.");
            } else {
                triggerChapterCue("ACT2_START", "2막 시작", 3800,
                        "시간이 흐를수록,",
                        "침묵은 조금씩 무거워졌다.");
            }
        }
    }

    public void releasePrivateEvidence(int revealOrder) {
        int order = clampEvidenceRevealOrder(revealOrder);
        if (order == privateEvidenceReleaseLevel + 1) {
            privateEvidenceReleaseLevel = order;
            state.setPhase(order == 1 ? GamePhase.FIRST_HINT : GamePhase.SECOND_HINT);
            if (order == 1) {
                triggerChapterCue("ACT1_LATE", "1막 후반", 3800,
                        "누구나",
                        "말하지 않은 이야기를 하나쯤 가지고 있다.");
            } else {
                triggerChapterCue("ACT3_START", "3막 시작", 3800,
                        "이제는",
                        "모른 척할 수 없는 것들이 남아 있었다.");
            }
        }
    }

    public EvidenceReleaseStatus evidenceReleaseStatus() {
        return new EvidenceReleaseStatus(publicEvidenceReleaseLevel, privateEvidenceReleaseLevel);
    }

    public List<AdminProgressStep> adminProgressSteps() {
        boolean storyDone = globalDisclosureStatus().storyUnlocked();
        boolean firstDiscussionDone = completedDiscussions.contains(GamePhase.FIRST_DISCUSSION);
        boolean secondDiscussionDone = completedDiscussions.contains(GamePhase.SECOND_DISCUSSION);
        boolean voteStarted = state.getPhase() == GamePhase.VOTE || voteClosed || endingRevealed;
        return List.of(
                adminStep(1, "개인 이야기 공개", storyDone, true, state.getPhase() == GamePhase.PRIVATE_STORY,
                        "/admin/reveal/story", null, null, "공개하기", "공개 완료", false),
                adminStep(2, "1차 토론 진행", firstDiscussionDone, storyDone, state.getPhase() == GamePhase.FIRST_DISCUSSION && !firstDiscussionDone,
                        "/admin/discussion/start", null, GamePhase.FIRST_DISCUSSION.name(), "토론 시작", "토론 완료", true),
                adminStep(3, "2차 토론 진행", secondDiscussionDone, firstDiscussionDone, state.getPhase() == GamePhase.SECOND_DISCUSSION && !secondDiscussionDone,
                        "/admin/discussion/start", null, GamePhase.SECOND_DISCUSSION.name(), "토론 시작", "토론 완료", true),
                adminStep(4, "투표 시작", voteStarted, secondDiscussionDone, state.getPhase() == GamePhase.VOTE && !voteClosed,
                        "/admin/vote/start", null, null, "투표 시작", "투표 시작됨", false),
                adminStep(5, "투표 종료", voteClosed || endingRevealed, voteStarted && !endingRevealed, voteClosed && !endingRevealed,
                        "/admin/vote/end", null, null, "투표 종료", "투표 종료됨", false),
                adminStep(6, "엔딩 공개", endingRevealed, voteClosed, endingRevealed,
                        "/admin/reveal/ending", null, null, "엔딩 공개", "엔딩 공개됨", false)
        );
    }

    public boolean canRunProgressStep(int order) {
        return adminProgressSteps().stream()
                .filter(step -> step.order() == order)
                .findFirst()
                .map(AdminProgressStep::enabled)
                .orElse(false);
    }

    private AdminProgressStep adminStep(int order, String title, boolean completed, boolean prerequisiteMet, boolean active,
                                        String action, Integer releaseOrder, String phaseName,
                                        String buttonLabel, String completedLabel, boolean timerControls) {
        boolean enabled = prerequisiteMet && !completed && !active;
        String status = completed ? "완료" : (active ? "진행 중" : (enabled ? "진행 가능" : "대기 중"));
        return new AdminProgressStep(order, title, status, enabled, completed, active, action, releaseOrder, phaseName,
                enabled ? buttonLabel : (completed ? completedLabel : "잠김"), timerControls);
    }

    public void resetGameState() {
        revealedHintIds.clear();
        sharedHintIds.clear();
        unlockedPersonalStoryCodes.clear();
        releasedHintPhases.clear();
        publicRecords.clear();
        votes.clear();
        publishedPrivateEvidence.clear();
        completedDiscussions.clear();
        publicEvidenceReleaseLevel = 0;
        privateEvidenceReleaseLevel = 0;
        endingRevealed = false;
        voteClosed = false;
        chapterCueSequence = 0;
        currentChapterCue = null;
        state.setPhase(GamePhase.WAITING);
        state.setTimerRunning(false);
        state.setTimerEndsAt(null);
        state.setTimerSeconds(600);
    }

    public List<PrivateEvidenceAdminStatus> privateEvidenceAdminStatuses() {
        return playableCharacters().stream()
                .map(player -> new PrivateEvidenceAdminStatus(
                        player,
                        simpleEvidenceItems().stream()
                                .filter(evidence -> evidence.type() == SimpleEvidenceType.PRIVATE)
                                .filter(evidence -> player.code().equals(evidence.characterCode()))
                                .sorted(java.util.Comparator.comparingInt(SimpleEvidence::displayOrder))
                                .map(evidence -> new PrivateEvidenceStatus(
                                        evidence.id(),
                                        evidence.title(),
                                        evidence.revealOrder(),
                                        privateEvidenceState(evidence)
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    public PublishEvidenceResult publishPrivateEvidence(Player player, String evidenceId) {
        if (player == null || player.code().equals("ADMIN")) {
            return new PublishEvidenceResult(false, "입장이 필요합니다.", true);
        }
        SimpleEvidence evidence = simpleEvidenceById(evidenceId).orElse(null);
        if (evidence == null || evidence.type() != SimpleEvidenceType.PRIVATE) {
            return new PublishEvidenceResult(false, "존재하지 않는 비공개 단서입니다.", true);
        }
        if (!player.code().equals(evidence.characterCode())) {
            return new PublishEvidenceResult(false, "본인이 받은 비공개 단서만 공개할 수 있습니다.", true);
        }
        if (evidence.revealOrder() > privateEvidenceReleaseLevel) {
            return new PublishEvidenceResult(false, "아직 지급되지 않은 비공개 단서입니다.", true);
        }
        if (publishedPrivateEvidence.containsKey(evidence.id())) {
            return new PublishEvidenceResult(true, "이미 모두에게 공개한 단서입니다.", false);
        }
        publishedPrivateEvidence.put(evidence.id(), new PublishedPrivateEvidence(
                evidence.id(),
                player.code(),
                Instant.now()
        ));
        return new PublishEvidenceResult(true, "비공개 단서를 모두에게 공개했습니다.", false);
    }

    public boolean isPrivateEvidencePublished(String evidenceId) {
        return publishedPrivateEvidence.containsKey(evidenceId);
    }

    public void startDiscussion(GamePhase phase, int minutes, int seconds) {
        if (phase != GamePhase.FIRST_DISCUSSION && phase != GamePhase.SECOND_DISCUSSION) {
            return;
        }
        completedDiscussions.remove(phase);
        setTimer(minutes, seconds);
        state.setPhase(phase);
        startTimer();
        if (phase == GamePhase.SECOND_DISCUSSION) {
            triggerChapterCue("SECOND_DISCUSSION", "2차 토론", 4200,
                    "누군가는 진실을 숨겼다.",
                    "누군가는",
                    "자기 자신에게조차 숨겼다.");
        }
    }

    public void finishDiscussion(GamePhase phase) {
        if (phase != GamePhase.FIRST_DISCUSSION && phase != GamePhase.SECOND_DISCUSSION) {
            return;
        }
        if (state.getPhase() != phase) {
            return;
        }
        completedDiscussions.add(phase);
        stopTimer();
        state.setTimerSeconds(0);
    }

    public void startVote() {
        voteClosed = false;
        state.setPhase(GamePhase.VOTE);
        stopTimer();
        triggerChapterCue("VOTE_START", "선택의 시간", 3800,
                "이제,",
                "한 사람을 선택해야 한다.");
    }

    public void closeVote() {
        if (state.getPhase() == GamePhase.VOTE) {
            voteClosed = true;
            stopTimer();
        }
    }

    private String privateEvidenceState(SimpleEvidence evidence) {
        if (publishedPrivateEvidence.containsKey(evidence.id())) {
            return "PUBLIC";
        }
        if (evidence.revealOrder() <= privateEvidenceReleaseLevel) {
            return "PRIVATE";
        }
        return "LOCKED";
    }

    private int clampEvidenceRevealOrder(int revealOrder) {
        return Math.max(1, Math.min(2, revealOrder));
    }

    private Optional<SimpleEvidence> simpleEvidenceById(String evidenceId) {
        return simpleEvidenceItems().stream()
                .filter(evidence -> evidence.id().equals(evidenceId))
                .findFirst();
    }

    private List<SimpleEvidence> simpleEvidenceItems() {
        return List.of(
                simpleEvidence("public-coat-button", SimpleEvidenceType.PUBLIC, null, 1, "뜯겨나간 외투 단추", "눈 덮인 숲길", "눈 속에서 작은 단추 하나가 발견되었다.\n\n단추에는\n토끼의 외투와 같은 천 조각이 붙어 있다.\n\n토끼의 외투 목깃 부분에는\n단추 하나가 사라져 있다.", "", 1),
                simpleEvidence("public-mixed-footprints", SimpleEvidenceType.PUBLIC, null, 1, "눈밭에 뒤섞인 발자국", "눈 덮인 숲길 — 고목 주변", "토끼가 쓰러져 있던 자리 주변에\n여러 방향의 발자국이 뒤섞여 있다.\n\n크기와 깊이가 서로 다르다.\n\n몇몇 발자국 주변에는\n젖은 재가 묻어 있다.", "", 2),
                simpleEvidence("public-scorched-snow", SimpleEvidenceType.PUBLIC, null, 1, "그을린 눈밭과 젖은 재", "눈 덮인 숲길", "토끼가 쓰러져 있던 자리 주변의 눈이\n군데군데 검게 그을려 있다.\n\n근처에는\n타다 만 나뭇가지와 젖은 재가 남아 있다.\n\n토끼의 옷자락 끝에도\n작은 그을림이 보인다.", "", 3),
                simpleEvidence("public-pocket-watch", SimpleEvidenceType.PUBLIC, null, 2, "11시 47분에 멈춘 회중시계", "눈 덮인 숲길", "눈 위에\n오래된 회중시계가 떨어져 있다.\n\n유리 표면에는 금이 가 있고,\n시곗바늘은 11시 47분에 멈춰 있다.", "", 4),
                simpleEvidence("public-torn-record", SimpleEvidenceType.PUBLIC, null, 2, "찢어진 기록 일부", "토끼의 방", "토끼가 남긴 기록 일부가\n거칠게 찢겨 있다.\n\n남아 있는 문장은 하나뿐이다.\n\n“…기억이 다시 흔들리기 시작했다.”\n\n종이 한쪽 끝에는\n희미한 그을림이 남아 있다.", "", 5),
                simpleEvidence("public-wood-shavings", SimpleEvidenceType.PUBLIC, null, 2, "시체 주변의 나무 가루", "눈 덮인 숲길", "토끼가 쓰러져 있던 자리 주변에\n미세한 나무 가루가 흩어져 있다.\n\n가루 일부에는\n희미한 바니시 냄새와\n매끄러운 코팅 흔적이 남아 있다.\n\n뜯겨나간 외투 단추 틈에서도\n같은 가루가 소량 발견되었다.", "", 6),
                simpleEvidence("alice-clock-sketch", SimpleEvidenceType.PRIVATE, "RED", 1, "반복된 시계 스케치", "앨리스의 방", "벽에는 회중시계 그림이\n반복해서 그려져 있다.\n\n모든 시계는\n11시 47분을 가리킨다.\n\n그림 아래에는\n같은 문장이 여러 번 적혀 있다.\n\n“멈추면 떠나지 않아.”", "앨리스는 사건이 알려지기 전부터\n11시 47분을 알고 있었던 것 같다.", 1),
                simpleEvidence("red-torn-cloth", SimpleEvidenceType.PRIVATE, "ALICE", 1, "찢어진 붉은 천 조각", "눈 덮인 숲길", "눈 속에서\n젖은 붉은 천 조각이 발견되었다.\n\n빨간망토가 입고 있는 망토와\n색과 질감이 비슷하다.", "빨간망토는 사건이 일어난 밤\n숲속에 있었던 것 같다.", 1),
                simpleEvidence("match-snowy-matchbox", SimpleEvidenceType.PRIVATE, "PINO", 1, "재와 눈이 묻은 성냥갑", "눈 덮인 숲길", "눈 속에서\n작은 성냥갑 하나가 발견되었다.\n\n표면에는\n검은 재와 녹은 눈이 묻어 있다.\n\n몇 개의 성냥은\n이미 사용되었다.\n\n성냥갑 뚜껑에는\n작은 별 모양 낙서가 남아 있다.", "성냥팔이 소녀가\n사건 현장에 다녀간 것 같다.", 1),
                simpleEvidence("pino-rabbit-note", SimpleEvidenceType.PRIVATE, "MATCH", 1, "토끼가 남긴 짧은 메모", "토끼의 방", "접힌 담요 아래에서\n작은 메모가 발견되었다.\n\n“마지막 아이를 데려오고 돌아오면,\n이번에는 네 이야기를 끝까지 들어줄게.”\n\n메모 아래에는\n목각소년의 이름이 적혀 있다.", "목각소년은 토끼가\n다른 아이를 데리러 간다는 사실을 알고 있었다.", 1),
                simpleEvidence("red-blood-palm", SimpleEvidenceType.PRIVATE, "RED", 2, "손바닥에만 남은 피", "자신의 손바닥 확인", "손바닥에\n말라붙은 피가 남아 있다.\n\n하지만 손목과 소매에는\n피가 묻어 있지 않다.", "피가 왜 묻었는지\n기억나지 않는다.", 2),
                simpleEvidence("alice-winding-key", SimpleEvidenceType.PRIVATE, "ALICE", 2, "기름과 실밥이 묻은 태엽 열쇠", "자신의 주머니 확인", "주머니 안에서\n작은 금속 열쇠가 발견되었다.\n\n표면에는 검은 기름이 묻어 있다.\n\n홈 사이에는\n가느다란 실밥 한 올이 끼어 있다.", "태엽 장치를 멈추려고\n열쇠에 손을 댄 기억이 떠오른다.\n\n반대 방향으로 돌렸던 것 같기도 하고,\n억지로 밀었던 것 같기도 하다.\n\n그 순간,\n실이 당겨지는 듯한 소리와\n무언가 끊어지는 듯한 소리를 들었다.", 2),
                simpleEvidence("pino-joint-thread", SimpleEvidenceType.PRIVATE, "PINO", 2, "관절 사이에 남은 실밥", "자신의 손목과 손가락 관절 확인", "손목과 손가락 관절 사이에\n가느다란 실밥이 끼어 있다.\n\n일부에는\n녹은 눈과 마른 나무 가루가 묻어 있다.", "이 실은 숨겨진 작업실의\n태엽 장치에 감겨 있던 질긴 실과 같은 재질이다.\n\n당신은 오늘 밤\n작업실에서 느슨하게 풀려 있던 실을 가져왔다.\n\n그리고 그 실을\n토끼의 목에 감았다.", 2),
                simpleEvidence("match-burned-blueprint", SimpleEvidenceType.PRIVATE, "MATCH", 2, "반쯤 탄 관절 설계도", "숨겨진 작업실", "반쯤 탄 종이 한 장이\n작업대 근처에 남아 있다.\n\n종이에는\n목각 인형처럼 보이는 몸과\n관절을 따라 이어지는 선이 그려져 있다.\n\n손목과 손가락 부분에는\n실을 여러 겹 감는 구조가 표시되어 있다.", "당신이 태운 종이다.\n\n토끼의 방에서 기록을 태운 뒤,\n자신의 방으로 돌아가던 길에\n열린 벽 틈을 발견했다.\n\n벽 너머에서 들리는 기계음을 따라\n숨겨진 작업실에 들어갔다.\n\n왜 이 설계도까지 태우려고 했는지는\n스스로도 확신하지 못한다.", 2)
        );
    }
    private SimpleEvidence simpleEvidence(String id, SimpleEvidenceType type, String characterCode, int revealOrder, String title, String locationText, String publicDescription, String privateInterpretation, int displayOrder) {
        return new SimpleEvidence(
                id,
                type,
                characterCode,
                revealOrder,
                title,
                locationText,
                publicDescription.strip(),
                privateInterpretation == null ? "" : privateInterpretation.strip(),
                displayOrder
        );
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
        if (isCorrectEnding()) {
            triggerChapterCue("ENDING_CORRECT", "사람이 되고 싶었던 인형", 6200,
                    "누군가는",
                    "끝까지 혼자 남고 싶지 않았다.",
                    "그래서 붙잡았다.",
                    "다시는 떠날 수 없도록.",
                    "하지만 토끼는",
                    "마지막 순간까지도",
                    "그를 괴물처럼 바라보지 않았다.");
        } else {
            triggerChapterCue("ENDING_WRONG", "잘못 선택된 괴물", 5600,
                    "사람들은 언제나",
                    "괴물을 찾고 싶어 했다.",
                    "진실보다 먼저,",
                    "두려워하기 쉬운 얼굴을 골랐다.");
        }
    }

    public boolean isEndingRevealed() {
        return endingRevealed;
    }

    public ChapterCue currentChapterCue() {
        return currentChapterCue;
    }

    private void triggerChapterCue(String code, String title, int durationMs, String... lines) {
        chapterCueSequence++;
        currentChapterCue = new ChapterCue(chapterCueSequence + ":" + code, title, List.of(lines), durationMs);
    }

    private boolean isCorrectEnding() {
        if (votes.isEmpty()) {
            return false;
        }
        return votes.values().stream()
                .collect(Collectors.groupingBy(code -> code, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> "PINO".equals(entry.getKey()))
                .orElse(false);
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

    public enum SimpleEvidenceType {
        PUBLIC,
        PRIVATE
    }

    public record SimpleEvidence(
            String id,
            SimpleEvidenceType type,
            String characterCode,
            int revealOrder,
            String title,
            String locationText,
            String publicDescription,
            String privateInterpretation,
            int displayOrder
    ) {
        public String description() {
            return publicDescription;
        }
    }

    public record PublishedPrivateEvidence(String evidenceId, String publishedByCharacterCode, Instant publishedAt) {
    }

    public record PublishedPrivateEvidenceView(String id, String title, String locationText, String publicDescription, String publishedByCharacterCode, String publishedByName, Instant publishedAt) {
    }

    public record EvidenceReleaseStatus(int publicEvidenceReleaseLevel, int privateEvidenceReleaseLevel) {
        public boolean firstPublicReleased() {
            return publicEvidenceReleaseLevel >= 1;
        }

        public boolean secondPublicReleased() {
            return publicEvidenceReleaseLevel >= 2;
        }

        public boolean firstPrivateReleased() {
            return privateEvidenceReleaseLevel >= 1;
        }

        public boolean secondPrivateReleased() {
            return privateEvidenceReleaseLevel >= 2;
        }
    }

    public record AdminProgressStep(
            int order,
            String title,
            String status,
            boolean enabled,
            boolean completed,
            boolean active,
            String action,
            Integer releaseOrder,
            String phaseName,
            String buttonLabel,
            boolean timerControls
    ) {
    }

    public record ChapterCue(String id, String title, List<String> lines, int durationMs) {
    }

    public record PrivateEvidenceAdminStatus(Player player, List<PrivateEvidenceStatus> items) {
    }

    public record PrivateEvidenceStatus(String id, String title, int revealOrder, String status) {
    }

    public record PublishEvidenceResult(boolean success, String message, boolean forbidden) {
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



