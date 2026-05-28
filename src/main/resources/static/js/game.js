const formatTime = (seconds) => {
    const safe = Math.max(0, Number(seconds) || 0);
    const minutes = Math.floor(safe / 60).toString().padStart(2, "0");
    const rest = Math.floor(safe % 60).toString().padStart(2, "0");
    return `${minutes}:${rest}`;
};

const showToast = (message) => {
    const toast = document.querySelector("[data-toast]");
    if (!toast) {
        alert(message);
        return;
    }
    toast.textContent = message;
    toast.hidden = false;
    window.clearTimeout(showToast.timeout);
    showToast.timeout = window.setTimeout(() => {
        toast.hidden = true;
    }, 4200);
};

const escapeHtml = (value) => String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#039;");

const postForm = async (url, data) => {
    const response = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/x-www-form-urlencoded"},
        body: new URLSearchParams(data)
    });
    return response.json();
};

const endingState = {
    source: "",
    page: 0,
    pages: []
};

let publicEvidence = [];
let personalHints = [];
let lastPhase = null;
let hintsInitialized = false;
let evidenceInitialized = false;

const seenHintIds = new Set(JSON.parse(window.sessionStorage.getItem("mysterySeenHints") || "[]"));
const seenEvidenceIds = new Set(JSON.parse(window.sessionStorage.getItem("mysterySeenEvidence") || "[]"));

const storyReaderState = {
    source: "",
    page: 0,
    pages: []
};

const actingReaderState = {
    source: "",
    page: 0,
    pages: []
};

const rememberSeen = (storageKey, seenSet, id) => {
    if (!id) {
        return;
    }
    seenSet.add(id);
    window.sessionStorage.setItem(storageKey, JSON.stringify([...seenSet]));
};

const splitEndingPages = (body) => {
    const paragraphs = String(body ?? "")
        .split(/\n+/)
        .map((text) => text.trim())
        .filter(Boolean);
    const pages = [];
    for (let index = 0; index < paragraphs.length; index += 3) {
        pages.push(paragraphs.slice(index, index + 3));
    }
    return pages.length > 0 ? pages : [[]];
};

const buildReaderChunks = (body, chunkSize = 3) => {
    const paragraphs = String(body ?? "")
        .split(/\n\s*\n+/)
        .map((text) => text.replace(/\s*\n\s*/g, " ").trim())
        .filter(Boolean);
    const units = paragraphs.length > 1
        ? paragraphs
        : String(body ?? "")
            .split(/(?<=[.!?。！？]|[다요죠까네음함됨임])\s+/)
            .map((text) => text.trim())
            .filter(Boolean);
    const chunks = [];
    for (let index = 0; index < units.length; index += chunkSize) {
        chunks.push(units.slice(index, index + chunkSize).join(" "));
    }
    return chunks.length > 0 ? chunks : [];
};

const splitReaderPages = (sections, size = 3) => {
    const pages = [];
    sections.forEach((section) => {
        const paragraphs = buildReaderChunks(section.body, size);
        for (let index = 0; index < paragraphs.length; index += 1) {
            pages.push({
                title: section.title,
                paragraphs: [paragraphs[index]]
            });
        }
    });
    return pages.length > 0 ? pages : [{title: "", paragraphs: []}];
};

const paintEndingPage = () => {
    const panel = document.querySelector("[data-ending-reveal]");
    if (!panel) {
        return;
    }
    const body = panel.querySelector("[data-ending-body]");
    const indicator = panel.querySelector("[data-ending-page-indicator]");
    const prev = panel.querySelector("[data-ending-prev]");
    const next = panel.querySelector("[data-ending-next]");
    const page = endingState.pages[endingState.page] || [];
    if (body) {
        body.classList.remove("is-turning");
        window.requestAnimationFrame(() => {
            body.classList.add("is-turning");
            body.innerHTML = page.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("");
        });
    }
    if (indicator) {
        indicator.textContent = `${endingState.page + 1} / ${endingState.pages.length}`;
    }
    if (prev) {
        prev.disabled = endingState.page === 0;
    }
    if (next) {
        next.disabled = endingState.page >= endingState.pages.length - 1;
        next.textContent = endingState.page >= endingState.pages.length - 1 ? "마지막 장" : "다음 장";
    }
};

const paintStoryPage = () => {
    const panel = document.querySelector("[data-story-content]");
    const controls = document.querySelector("[data-story-controls]");
    if (!panel || !controls || storyReaderState.pages.length === 0) {
        return;
    }
    const page = storyReaderState.pages[storyReaderState.page] || storyReaderState.pages[0];
    const renderKey = `${storyReaderState.source}:${storyReaderState.page}`;
    if (panel.dataset.renderKey !== renderKey) {
        panel.dataset.renderKey = renderKey;
        panel.classList.remove("is-turning");
        window.requestAnimationFrame(() => {
            panel.classList.add("is-turning");
            panel.innerHTML = `
                ${page.title ? `<h3>${escapeHtml(page.title)}</h3>` : ""}
                ${page.paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}
            `;
        });
    }
    controls.hidden = false;
    const indicator = controls.querySelector("[data-story-page-indicator]");
    const prev = controls.querySelector("[data-story-prev]");
    const next = controls.querySelector("[data-story-next]");
    if (indicator) {
        indicator.textContent = `${storyReaderState.page + 1} / ${storyReaderState.pages.length}`;
    }
    if (prev) {
        prev.disabled = storyReaderState.page === 0;
    }
    if (next) {
        next.disabled = storyReaderState.page >= storyReaderState.pages.length - 1;
        next.textContent = storyReaderState.page >= storyReaderState.pages.length - 1 ? "마지막 장" : "다음 장";
    }
};

const paintActingPage = () => {
    const panel = document.querySelector("[data-acting-content]");
    const controls = document.querySelector("[data-acting-controls]");
    if (!panel || !controls || actingReaderState.pages.length === 0) {
        return;
    }
    const page = actingReaderState.pages[actingReaderState.page] || actingReaderState.pages[0];
    const renderKey = `${actingReaderState.source}:${actingReaderState.page}`;
    if (panel.dataset.renderKey !== renderKey) {
        panel.dataset.renderKey = renderKey;
        panel.classList.remove("is-turning");
        window.requestAnimationFrame(() => {
            panel.classList.add("is-turning");
            panel.innerHTML = `
                ${page.title ? `<h3>${escapeHtml(page.title)}</h3>` : ""}
                ${page.paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}
            `;
        });
    }
    controls.hidden = false;
    const indicator = controls.querySelector("[data-acting-page-indicator]");
    const prev = controls.querySelector("[data-acting-prev]");
    const next = controls.querySelector("[data-acting-next]");
    if (indicator) {
        indicator.textContent = `${actingReaderState.page + 1} / ${actingReaderState.pages.length}`;
    }
    if (prev) {
        prev.disabled = actingReaderState.page === 0;
    }
    if (next) {
        next.disabled = actingReaderState.page >= actingReaderState.pages.length - 1;
        next.textContent = actingReaderState.page >= actingReaderState.pages.length - 1 ? "마지막 장" : "다음 장";
    }
};

const showPhaseCurtain = (phaseLabel, phaseKey) => {
    const curtain = document.querySelector("[data-phase-curtain]");
    if (!curtain) {
        return;
    }
    const title = curtain.querySelector("p");
    const note = curtain.querySelector("span");
    const lines = {
        WAITING: ["프롤로그 종료", "하지만 이번에는, 토끼가 돌아오지 못했다."],
        STORY_REVEAL: ["1막 시작", "괴물은 언제나 가장 두려운 얼굴을 하고 있다."],
        FIRST_DISCUSSION: ["1막 시작", "괴물은 언제나 가장 두려운 얼굴을 하고 있다."],
        FIRST_HINT: ["1막의 기록", "누군가의 흔적이 너무 쉽게 남아 있었다."],
        SECOND_DISCUSSION: ["2막 시작", "같은 장면을 보았는데도, 모두의 이야기는 조금씩 달랐다."],
        SECOND_HINT: ["2막의 기록", "기억은 가끔 남이 쓴 문장처럼 흔들린다."],
        THIRD_HINT: ["3막 시작", "숨겨져 있던 문이 열린다."],
        FINAL_DISCUSSION: ["최종 토론", "당신들이 본 것은 정말 현실이었을까."],
        VOTE: ["투표 직전", "괴물은 누구인가."],
        ENDING: ["마지막 장", "누군가는 끝까지 혼자 남고 싶지 않았다."]
    };
    const selected = lines[phaseKey] || [phaseLabel || "다음 막", "눈발 사이로 장면이 바뀝니다."];
    if (title) {
        title.textContent = selected[0];
    }
    if (note) {
        note.textContent = selected[1];
    }
    curtain.hidden = false;
    curtain.classList.remove("is-fading");
    window.clearTimeout(showPhaseCurtain.timeout);
    showPhaseCurtain.timeout = window.setTimeout(() => {
        curtain.classList.add("is-fading");
        window.setTimeout(() => {
            curtain.hidden = true;
            curtain.classList.remove("is-fading");
        }, 520);
    }, 1500);
};

const initStaticReader = () => {
    const reader = document.querySelector("[data-static-reader]");
    if (!reader || reader.dataset.readerReady) {
        return;
    }
    reader.dataset.readerReady = "true";
    const body = reader.querySelector("[data-static-reader-body]");
    const finish = reader.querySelector("[data-static-reader-finish]");
    const indicator = reader.querySelector("[data-static-reader-indicator]");
    const prev = reader.querySelector("[data-static-reader-prev]");
    const next = reader.querySelector("[data-static-reader-next]");
    const sourceText = Array.from(body?.querySelectorAll("p") || [])
        .map((node) => node.textContent.trim())
        .filter(Boolean)
        .join("\n\n");
    const storageKey = `mystery-reader-${reader.dataset.readerKey || "story"}`;
    const pages = [];
    const sourceParagraphs = buildReaderChunks(sourceText, 3);
    for (let index = 0; index < sourceParagraphs.length; index += 1) {
        pages.push([sourceParagraphs[index]]);
    }
    let pageIndex = Math.min(Number(window.sessionStorage.getItem(storageKey)) || 0, Math.max(0, pages.length - 1));
    const paint = () => {
        const page = pages[pageIndex] || [];
        if (body) {
            body.classList.remove("is-turning");
            window.requestAnimationFrame(() => {
                body.classList.add("is-turning");
                body.innerHTML = page.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("");
            });
        }
        if (indicator) {
            indicator.textContent = `${pageIndex + 1} / ${pages.length || 1}`;
        }
        if (prev) {
            prev.disabled = pageIndex === 0;
        }
        if (next) {
            next.disabled = pageIndex >= pages.length - 1;
            next.textContent = pageIndex >= pages.length - 1 ? "마지막 장" : "다음 장";
        }
        if (finish) {
            finish.hidden = pageIndex < pages.length - 1;
        }
        window.sessionStorage.setItem(storageKey, String(pageIndex));
    };
    prev?.addEventListener("click", () => {
        pageIndex = Math.max(0, pageIndex - 1);
        paint();
    });
    next?.addEventListener("click", () => {
        pageIndex = Math.min(pages.length - 1, pageIndex + 1);
        paint();
    });
    paint();
};

const renderHints = async () => {
    const lists = document.querySelectorAll("[data-hint-list]");
    if (lists.length === 0) {
        return;
    }
    const response = await fetch("/api/hints", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    personalHints = await response.json();
    if (!hintsInitialized) {
        hintsInitialized = true;
        personalHints.forEach((hint) => {
            if (seenHintIds.has(hint.id)) {
                return;
            }
            if (!hint.id.startsWith("evidence-")) {
                rememberSeen("mysterySeenHints", seenHintIds, hint.id);
            }
        });
    }
    const renderGroup = (title, typeCode, list) => {
        const items = personalHints.filter((hint) => hint.typeCode === typeCode);
        list.innerHTML = `
            ${items.map((hint) => {
                const isNew = !seenHintIds.has(hint.id);
                return `
                <article class="hint-note compact ${isNew ? "is-new" : ""}" data-hint-id="${escapeHtml(hint.id)}">
                    <button class="hint-open" data-hint-open="${escapeHtml(hint.id)}" type="button">
                        <span>${escapeHtml(hint.roundLabel)} · ${escapeHtml(hint.type)} ${isNew ? `<b class="new-badge">NEW</b>` : ""}</span>
                        <strong>${escapeHtml(hint.title)}</strong>
                    </button>
                    <div class="share-area">
                        <p>공개하면 되돌릴 수 없습니다.</p>
                        <button class="button ghost small share-button" data-share-hint="${escapeHtml(hint.id)}" ${hint.shared ? "disabled" : ""} type="button">
                            ${hint.shared ? "공개됨" : `${title} 공개`}
                        </button>
                    </div>
                </article>`;
            }).join("")}
        `;
        document.querySelectorAll(`[data-empty-type="${typeCode}"]`).forEach((node) => {
            node.hidden = items.length > 0;
        });
    };
    lists.forEach((list) => {
        const typeCode = list.dataset.hintType || "TESTIMONY";
        renderGroup(typeCode === "EVIDENCE" ? "물증" : "증언", typeCode, list);
    });
};

const openHintDetail = (hint) => {
    const panel = document.querySelector("[data-evidence-detail]");
    if (!hint || !panel) {
        return;
    }
    rememberSeen("mysterySeenHints", seenHintIds, hint.id);
    panel.querySelector("[data-evidence-meta]").textContent = `${hint.roundLabel} · ${hint.type}`;
    panel.querySelector("[data-evidence-title]").textContent = hint.title;
    panel.querySelector("[data-evidence-short]").textContent = hint.shared ? "공개된 항목입니다." : "아직 다른 플레이어에게 공개하지 않았습니다.";
    panel.querySelector("[data-evidence-keywords]").innerHTML = `<em>${escapeHtml(hint.type)}</em><em>${escapeHtml(hint.roundLabel)}</em>`;
    panel.querySelector("[data-evidence-body]").textContent = hint.body;
    panel.hidden = false;
};

const renderStory = async () => {
    const locked = document.querySelector("[data-story-locked]");
    const content = document.querySelector("[data-story-content]");
    const actingLocked = document.querySelector("[data-acting-locked]");
    const actingContent = document.querySelector("[data-acting-content]");
    if (!locked || !content) {
        return;
    }
    const response = await fetch("/api/story", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const story = await response.json();
    locked.hidden = story.revealed;
    content.hidden = !story.revealed;
    if (actingLocked) {
        actingLocked.hidden = story.revealed;
    }
    if (actingContent) {
        actingContent.hidden = !story.revealed;
    }
    if (story.revealed) {
        const source = JSON.stringify([story.personalStory, story.secret]);
        if (storyReaderState.source !== source) {
            storyReaderState.source = source;
            storyReaderState.pages = splitReaderPages([
                {title: "개인 이야기", body: story.personalStory},
                {title: "비밀 정보", body: story.secret}
            ]);
            storyReaderState.page = Math.min(storyReaderState.page, storyReaderState.pages.length - 1);
        }
        paintStoryPage();
        const actingSource = story.actingHint || "아직 별도의 연기 힌트가 없습니다.";
        if (actingReaderState.source !== actingSource) {
            actingReaderState.source = actingSource;
            actingReaderState.pages = splitReaderPages([
                {title: "연기 힌트", body: actingSource}
            ], 2);
            actingReaderState.page = Math.min(actingReaderState.page, actingReaderState.pages.length - 1);
        }
        paintActingPage();
    } else {
        document.querySelector("[data-story-controls]")?.setAttribute("hidden", "");
        document.querySelector("[data-acting-controls]")?.setAttribute("hidden", "");
    }
};

const renderPublicStatuses = async () => {
    const strip = document.querySelector("[data-public-statuses]");
    if (!strip) {
        return;
    }
    const response = await fetch("/api/public-statuses", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const statuses = await response.json();
    strip.innerHTML = statuses.map((status) => `
        <div class="${status.self ? "self" : ""}">
            <strong>${escapeHtml(status.name)}</strong>
            <span class="${status.hintsShared ? "open" : ""}" title="개인 힌트">힌트</span>
        </div>
    `).join("");
};

const renderPublicRecords = async () => {
    const list = document.querySelector("[data-public-record-list]");
    if (!list) {
        return;
    }
    const response = await fetch("/api/public-records", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const records = await response.json();
    list.innerHTML = records.map((record) => `
        <article class="record-card">
            <span>${escapeHtml(record.playerName)} 공개 · ${escapeHtml(record.type)} · ${escapeHtml(record.phaseLabel)}</span>
            <h3>${escapeHtml(record.title)}</h3>
            <p>${escapeHtml(record.body)}</p>
        </article>
    `).join("");
    document.querySelectorAll("[data-empty-records]").forEach((node) => {
        node.hidden = records.length > 0;
    });
};

const evidenceIcon = (visual) => {
    const icons = {
        claw: "╱╱",
        cloth: "▰",
        blood: "●",
        paper: "※",
        ink: "≈",
        memory: "…",
        portrait: "◌",
        watch: "◷",
        thread: "⌁",
        wood: "◇",
        blueprint: "⌗",
        doll: "◉",
        fur: "≋",
        ash: "✕",
        footprint: "⌾"
    };
    return icons[visual] || "◆";
};

const evidenceImage = (itemOrVisual) => {
    const id = typeof itemOrVisual === "object" ? itemOrVisual.id : "";
    const visual = typeof itemOrVisual === "object" ? itemOrVisual.visual : itemOrVisual;
    const byId = {
        "claw-marks": "blood-watch.png",
        "torn-red-cloth": "torn-invitation.png",
        "fading-blood": "blood-watch.png",
        "wet-wolf-fur": "red-thread.png",
        "broken-door-handle": "wood-shavings.png",
        "torn-record": "torn-invitation.png",
        "blurred-ink": "sealed-letter.png",
        "repeated-sentence": "sealed-letter.png",
        "wall-memory-scratch": "sealed-letter.png",
        "damaged-group-picture": "card-fragment.png",
        "blurred-face": "card-fragment.png",
        "scratched-watch": "blood-watch.png",
        "clock-sound-conflict": "blood-watch.png",
        "broken-thread": "red-thread.png",
        "wood-dust": "wood-shavings.png",
        "burned-blueprint": "sealed-letter.png",
        "cracked-doll-eye": "wood-shavings.png",
        "future-note": "torn-invitation.png",
        "red-rabbit-drawing": "card-fragment.png",
        "half-burned-paper": "matchbox.png",
        "black-ash": "matchbox.png",
        "conflicting-times": "blood-watch.png",
        "silent-footprints": "torn-invitation.png",
        "laughing-witness": "sealed-letter.png",
        "rabbit-room-record": "sealed-letter.png",
        "rabbit-desk-record": "sealed-letter.png",
        "rabbit-workshop-record": "wood-shavings.png"
    };
    const images = {
        claw: "blood-watch.png",
        cloth: "torn-invitation.png",
        blood: "blood-watch.png",
        paper: "torn-invitation.png",
        ink: "sealed-letter.png",
        memory: "sealed-letter.png",
        portrait: "card-fragment.png",
        watch: "blood-watch.png",
        thread: "red-thread.png",
        wood: "wood-shavings.png",
        blueprint: "sealed-letter.png",
        doll: "wood-shavings.png",
        fur: "red-thread.png",
        ash: "matchbox.png",
        footprint: "torn-invitation.png"
    };
    return `/images/evidence/${byId[id] || images[visual] || "sealed-letter.png"}`;
};

const renderEvidence = async () => {
    const list = document.querySelector("[data-evidence-list]");
    if (!list) {
        return;
    }
    const response = await fetch("/api/evidence", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    publicEvidence = await response.json();
    if (!evidenceInitialized) {
        evidenceInitialized = true;
        publicEvidence.forEach((item) => rememberSeen("mysterySeenEvidence", seenEvidenceIds, item.id));
    }
    list.innerHTML = publicEvidence.map((item) => {
        const isNew = !seenEvidenceIds.has(item.id);
        return `
            <button class="evidence-card evidence-${escapeHtml(item.visual)} ${isNew ? "is-new" : ""}" data-evidence-id="${escapeHtml(item.id)}" type="button">
                <span class="evidence-photo" style="background-image: url('${escapeHtml(evidenceImage(item))}')"></span>
                <span class="evidence-mark">${escapeHtml(evidenceIcon(item.visual))}</span>
                <strong>${escapeHtml(item.name)} ${isNew ? `<b class="new-badge">NEW</b>` : ""}</strong>
                <small>${escapeHtml(item.revealRound)} · ${escapeHtml(item.category)}</small>
                <p>${escapeHtml(item.shortDescription)}</p>
                <span class="evidence-tags">${(item.keywords || []).map((keyword) => `<em>${escapeHtml(keyword)}</em>`).join("")}</span>
            </button>
        `;
    }).join("");
    document.querySelectorAll("[data-empty-evidence]").forEach((node) => {
        node.hidden = publicEvidence.length > 0;
    });
};

const renderEnding = (state) => {
    const panel = document.querySelector("[data-ending-reveal]");
    if (!panel) {
        return;
    }
    panel.hidden = !state.endingRevealed;
    if (!state.endingRevealed) {
        return;
    }
    document.querySelectorAll("[data-panel]").forEach((node) => {
        node.hidden = true;
    });
    document.querySelectorAll("[data-vote]").forEach((node) => {
        node.hidden = true;
    });
    const title = panel.querySelector("[data-ending-title]");
    const body = panel.querySelector("[data-ending-body]");
    if (title) {
        title.textContent = state.endingTitle || "토끼가 남긴 마지막 시간";
    }
    if (body && endingState.source !== state.endingBody) {
        endingState.source = state.endingBody || "";
        endingState.page = 0;
        endingState.pages = splitEndingPages(endingState.source);
        paintEndingPage();
    }
};

const refreshState = async () => {
    const response = await fetch("/api/state", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const state = await response.json();
    if (lastPhase && lastPhase !== state.phase) {
        showPhaseCurtain(state.phaseLabel, state.phase);
    }
    lastPhase = state.phase;
    document.querySelectorAll("[data-timer]").forEach((node) => {
        node.textContent = formatTime(state.timerSeconds);
    });
    document.querySelectorAll("[data-phase-label]").forEach((node) => {
        node.textContent = state.phaseLabel;
    });
    document.querySelectorAll("[data-phase-description]").forEach((node) => {
        node.textContent = state.phaseDescription;
    });
    document.querySelectorAll("[data-story-locked]").forEach((node) => {
        node.hidden = state.personalStoryRevealed;
    });
    document.querySelectorAll("[data-vote]").forEach((node) => {
        node.hidden = state.phase !== "VOTE";
    });
    document.body.dataset.phase = state.phase;
    renderEnding(state);
    await renderHints();
    await renderStory();
    await renderPublicStatuses();
    await renderPublicRecords();
    await renderEvidence();
};

document.addEventListener("submit", async (event) => {
    const form = event.target.closest("[data-vote]");
    if (!form) {
        return;
    }
    event.preventDefault();
    const result = await postForm("/api/vote", {
        suspectCode: new FormData(form).get("suspectCode")
    });
    showToast(result.message);
});

document.addEventListener("submit", async (event) => {
    const form = event.target.closest("form[action^='/admin/']");
    if (!form) {
        return;
    }
    event.preventDefault();
    const scrollX = window.scrollX;
    const scrollY = window.scrollY;
    const submitButton = event.submitter;
    if (submitButton) {
        submitButton.disabled = true;
    }
    try {
        const response = await fetch(form.action, {
            method: "POST",
            headers: {"Content-Type": "application/x-www-form-urlencoded"},
            body: new URLSearchParams(new FormData(form))
        });
        if (!response.ok) {
            showToast("처리하지 못했습니다.");
            return;
        }
        const htmlResponse = await fetch("/admin", {headers: {"Accept": "text/html"}});
        if (htmlResponse.ok) {
            const html = await htmlResponse.text();
            const doc = new DOMParser().parseFromString(html, "text/html");
            const nextHeader = doc.querySelector(".topbar");
            const nextMain = doc.querySelector(".admin-grid");
            const header = document.querySelector(".topbar");
            const main = document.querySelector(".admin-grid");
            if (nextHeader && header) {
                header.innerHTML = nextHeader.innerHTML;
            }
            if (nextMain && main) {
                main.innerHTML = nextMain.innerHTML;
            }
        }
        await refreshState();
        window.scrollTo(scrollX, scrollY);
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
        }
    }
});

document.addEventListener("click", async (event) => {
    const hintShareButton = event.target.closest("[data-share-hint]");
    if (hintShareButton) {
        if (!window.confirm("공개하면 모든 플레이어가 볼 수 있으며 되돌릴 수 없습니다. 공개할까요?")) {
            return;
        }
        postForm("/api/share/hint", {hintId: hintShareButton.dataset.shareHint}).then(async (result) => {
            showToast(result.message);
            await renderHints();
            await renderPublicStatuses();
            await renderPublicRecords();
            await renderEvidence();
        });
        return;
    }

    const hintOpenButton = event.target.closest("[data-hint-open]");
    if (hintOpenButton) {
        openHintDetail(personalHints.find((hint) => hint.id === hintOpenButton.dataset.hintOpen));
        return;
    }

    const panelButton = event.target.closest("[data-panel-target]");
    if (panelButton) {
        const target = document.getElementById(panelButton.dataset.panelTarget);
        if (target && (target.id === "story-panel" || target.id === "acting-panel")) {
            await renderStory();
        }
        document.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.hidden = panel !== target;
        });
        if (target) {
            target.hidden = false;
            if (target.id === "story-panel") {
                paintStoryPage();
            }
            if (target.id === "acting-panel") {
                paintActingPage();
            }
        }
        return;
    }

    const collapseButton = event.target.closest("[data-collapse-target]");
    if (collapseButton) {
        const board = collapseButton.closest(".collapsible-board");
        if (board) {
            board.classList.toggle("is-collapsed");
            collapseButton.textContent = board.classList.contains("is-collapsed") ? "펼치기" : "접기";
        }
        return;
    }

    if (event.target.closest("[data-panel-close]")) {
        document.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.hidden = true;
        });
        return;
    }

    if (event.target.closest("[data-evidence-close]")) {
        document.querySelectorAll("[data-evidence-detail]").forEach((panel) => {
            panel.hidden = true;
        });
        return;
    }

    const evidenceButton = event.target.closest("[data-evidence-id]");
    if (evidenceButton) {
        const item = publicEvidence.find((entry) => entry.id === evidenceButton.dataset.evidenceId);
        const panel = document.querySelector("[data-evidence-detail]");
        if (item && panel) {
            rememberSeen("mysterySeenEvidence", seenEvidenceIds, item.id);
            panel.querySelector("[data-evidence-meta]").textContent = `${item.revealRound} · ${item.category}`;
            panel.querySelector("[data-evidence-title]").textContent = item.name;
            panel.querySelector("[data-evidence-short]").textContent = item.shortDescription;
            panel.querySelector("[data-evidence-keywords]").innerHTML = (item.keywords || []).map((keyword) => `<em>${escapeHtml(keyword)}</em>`).join("");
            panel.querySelector("[data-evidence-body]").textContent = item.detail;
            const image = panel.querySelector("[data-evidence-image]");
            if (image) {
                image.src = evidenceImage(item);
            }
            panel.hidden = false;
        }
        return;
    }

    if (event.target.closest("[data-ending-prev]")) {
        endingState.page = Math.max(0, endingState.page - 1);
        paintEndingPage();
        return;
    }

    if (event.target.closest("[data-ending-next]")) {
        endingState.page = Math.min(endingState.pages.length - 1, endingState.page + 1);
        paintEndingPage();
        return;
    }

    if (event.target.closest("[data-story-prev]")) {
        storyReaderState.page = Math.max(0, storyReaderState.page - 1);
        paintStoryPage();
        return;
    }

    if (event.target.closest("[data-story-next]")) {
        storyReaderState.page = Math.min(storyReaderState.pages.length - 1, storyReaderState.page + 1);
        paintStoryPage();
        return;
    }

    if (event.target.closest("[data-acting-prev]")) {
        actingReaderState.page = Math.max(0, actingReaderState.page - 1);
        paintActingPage();
        return;
    }

    if (event.target.closest("[data-acting-next]")) {
        actingReaderState.page = Math.min(actingReaderState.pages.length - 1, actingReaderState.page + 1);
        paintActingPage();
        return;
    }

    const shadow = event.target.closest(".character-shadow");
    if (shadow) {
        document.querySelectorAll(".character-shadow.is-fixed").forEach((node) => {
            if (node !== shadow) {
                node.classList.remove("is-fixed");
            }
        });
        shadow.classList.toggle("is-fixed");
        window.clearTimeout(shadow.fixedTimeout);
        if (shadow.classList.contains("is-fixed")) {
            shadow.fixedTimeout = window.setTimeout(() => {
                shadow.classList.remove("is-fixed");
            }, 4500);
        }
    }
});

initStaticReader();
refreshState();
window.setInterval(refreshState, 1000);
