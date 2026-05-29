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

const paragraphsHtml = (value) => String(value ?? "")
    .split(/\n+/)
    .map((text) => text.trim())
    .filter(Boolean)
    .map((text) => `<p>${escapeHtml(text)}</p>`)
    .join("");

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

const openMemoryEvidence = (button) => {
    const modal = document.querySelector("[data-memory-modal]");
    if (!modal || !button) {
        return;
    }
    modal.querySelector("[data-memory-title]").textContent = button.dataset.memoryTitle || "";
    modal.querySelector("[data-memory-location]").textContent = button.dataset.memoryLocation || "";
    modal.querySelector("[data-memory-common]").innerHTML = paragraphsHtml(button.dataset.memoryCommon || "");
    modal.querySelector("[data-memory-personal]").innerHTML = paragraphsHtml(button.dataset.memoryPersonal || "이 장면에서 당신에게 떠오르는 해석은 아직 없습니다.");

    const recalledWrap = modal.querySelector("[data-memory-recalled-wrap]");
    const recalled = button.dataset.memoryRecalled || "";
    modal.querySelector("[data-memory-recalled]").innerHTML = paragraphsHtml(recalled);
    if (recalledWrap) {
        recalledWrap.hidden = !recalled.trim();
    }
    modal.hidden = false;
    document.body.classList.add("memory-modal-open");
};

const closeMemoryEvidence = () => {
    const modal = document.querySelector("[data-memory-modal]");
    if (modal) {
        modal.hidden = true;
    }
    document.body.classList.remove("memory-modal-open");
};

const investigationState = {
    locations: window.investigationInitialState?.locations || [],
    privateEvidence: window.investigationInitialState?.privateEvidence || [],
    publicEvidence: window.investigationInitialState?.publicEvidence || [],
    activeLocationId: "",
    menuEvidenceId: "",
    pendingPublishId: ""
};

const openMemoryEvidenceDetail = (evidence) => {
    const modal = document.querySelector("[data-memory-modal]");
    if (!modal || !evidence) {
        return;
    }
    modal.querySelector("[data-memory-title]").textContent = evidence.title || "";
    modal.querySelector("[data-memory-location]").textContent = evidence.location || "";
    modal.querySelector("[data-memory-common]").innerHTML = paragraphsHtml(evidence.commonDescription || "");
    modal.querySelector("[data-memory-personal]").innerHTML = paragraphsHtml(evidence.personalInterpretation || "이 장면에서 당신에게 떠오르는 해석은 아직 없습니다.");

    const recalledWrap = modal.querySelector("[data-memory-recalled-wrap]");
    const recalled = evidence.recalledLine || "";
    modal.querySelector("[data-memory-recalled]").innerHTML = paragraphsHtml(recalled);
    if (recalledWrap) {
        recalledWrap.hidden = !recalled.trim();
    }
    modal.hidden = false;
    document.body.classList.add("memory-modal-open");
};

const refreshInvestigation = async () => {
    const root = document.querySelector("[data-investigation-root]");
    const hasLedger = document.querySelector("[data-private-evidence]") || document.querySelector("[data-public-evidence]");
    if (!root && !hasLedger) {
        return;
    }
    const response = await fetch("/api/investigation");
    if (!response.ok) {
        return;
    }
    const data = await response.json();
    if (!data.success) {
        return;
    }
    investigationState.locations = data.locations || [];
    investigationState.privateEvidence = data.privateEvidence || [];
    investigationState.publicEvidence = data.publicEvidence || [];
    if (!investigationState.locations.some((location) => location.id === investigationState.activeLocationId && location.occupiedBySelf)) {
        const ownLocation = investigationState.locations.find((location) => location.occupiedBySelf);
        investigationState.activeLocationId = ownLocation?.id || "";
    }
    renderInvestigation();
};

const renderInvestigation = () => {
    const root = document.querySelector("[data-investigation-root]");
    if (!root) {
        renderEvidenceLedger("[data-private-evidence]", investigationState.privateEvidence, "private");
        renderEvidenceLedger("[data-public-evidence]", investigationState.publicEvidence, "public");
        return;
    }
    const locationWrap = root.querySelector(".investigation-locations");
    if (locationWrap) {
        locationWrap.innerHTML = investigationState.locations.map((location) => `
            <article class="scene-file investigation-location ${location.occupiedBySelf ? "is-active" : ""}" data-location-id="${escapeHtml(location.id)}">
                <span>${escapeHtml(location.act)}막 장면</span>
                <strong>${escapeHtml(location.name)}</strong>
                <em>${escapeHtml(location.description)}</em>
                <p class="location-state">${locationText(location)}</p>
                <button class="button primary small" type="button" data-location-enter data-location-id="${escapeHtml(location.id)}" ${location.canEnter ? "" : "disabled"}>
                    ${location.occupiedBySelf ? "다시 들어가기" : "조사하기"}
                </button>
            </article>
        `).join("");
    }

    const active = investigationState.locations.find((location) => location.id === investigationState.activeLocationId && location.occupiedBySelf);
    const room = root.querySelector("[data-investigation-room]");
    if (room) {
        room.querySelector("[data-room-kicker]").textContent = active ? `${active.act}막 장면` : "사건 장면";
        room.querySelector("[data-room-title]").textContent = active ? active.name : "조사할 장면을 선택하세요";
        room.querySelector("[data-room-summary]").textContent = active ? active.description : "한 장면에는 한 명만 들어갈 수 있습니다. 조사 중 발견한 증거는 먼저 내 손에 남습니다.";
        const evidenceWrap = room.querySelector("[data-room-evidence]");
        evidenceWrap.innerHTML = active ? (active.evidenceItems || []).map((evidence) => `
            <button class="memory-evidence-card ${evidence.privateFound ? "is-private-found" : ""} ${evidence.publicItem ? "is-public" : ""}"
                    type="button"
                    data-investigation-evidence
                    data-evidence-id="${escapeHtml(evidence.id)}">
                <span>${evidence.publicItem ? "공개된 흔적" : evidence.privateFound ? "내가 발견한 흔적" : "남겨진 흔적"}</span>
                <strong>${escapeHtml(evidence.title)}</strong>
                <em>${escapeHtml(evidence.detailLocation)}</em>
            </button>
        `).join("") : "";
        const leave = room.querySelector("[data-location-leave]");
        leave.hidden = !active;
        if (active) {
            leave.dataset.locationId = active.id;
        }
    }

    renderEvidenceLedger("[data-private-evidence]", investigationState.privateEvidence, "private");
    renderEvidenceLedger("[data-public-evidence]", investigationState.publicEvidence, "public");
};

const locationText = (location) => {
    if (!location.open) {
        return "아직 공개되지 않음";
    }
    if (location.occupiedBySelf) {
        return "내가 조사 중";
    }
    if (location.occupied) {
        return `현재 ${escapeHtml(location.occupiedByName || "다른 플레이어")} 조사 중`;
    }
    return "조사 가능";
};

const renderEvidenceLedger = (selector, items, type) => {
    const wrap = document.querySelector(selector);
    if (!wrap) {
        return;
    }
    wrap.innerHTML = (items || []).map((evidence) => `
        <button class="ledger-evidence ${type === "public" ? "is-public" : ""}"
                type="button"
                ${type === "private" ? "data-private-evidence-card" : "data-public-evidence-card"}
                data-evidence-id="${escapeHtml(evidence.id)}">
            <strong>${escapeHtml(evidence.title)}</strong>
            <span>${escapeHtml(evidence.locationName)} - ${escapeHtml(evidence.detailLocation)}</span>
        </button>
    `).join("");
};

const fetchEvidenceDetail = async (evidenceId, discover = false) => {
    const response = discover
        ? await postForm("/api/investigation/evidence/discover", {evidenceId})
        : await fetch(`/api/investigation/evidence/detail?evidenceId=${encodeURIComponent(evidenceId)}`).then((item) => item.json());
    if (!response.success) {
        showToast(response.message || "증거를 펼칠 수 없습니다.");
        return null;
    }
    return response.evidence;
};

const openEvidenceContextMenu = (evidenceId, x, y) => {
    const menu = document.querySelector("[data-evidence-menu]");
    if (!menu) {
        return;
    }
    investigationState.menuEvidenceId = evidenceId;
    menu.hidden = false;
    const width = menu.offsetWidth || 150;
    const height = menu.offsetHeight || 120;
    menu.style.left = `${Math.min(x, window.innerWidth - width - 12)}px`;
    menu.style.top = `${Math.min(y, window.innerHeight - height - 12)}px`;
};

const closeEvidenceContextMenu = () => {
    const menu = document.querySelector("[data-evidence-menu]");
    if (menu) {
        menu.hidden = true;
    }
    investigationState.menuEvidenceId = "";
};

const openPublishConfirm = (evidenceId) => {
    const modal = document.querySelector("[data-publish-confirm]");
    const evidence = investigationState.privateEvidence.find((item) => item.id === evidenceId);
    if (!modal || !evidence) {
        return;
    }
    investigationState.pendingPublishId = evidenceId;
    modal.querySelector("[data-confirm-title]").textContent = `[${evidence.title}]을 공개하시겠습니까?`;
    modal.hidden = false;
};

const closePublishConfirm = () => {
    const modal = document.querySelector("[data-publish-confirm]");
    if (modal) {
        modal.hidden = true;
    }
    investigationState.pendingPublishId = "";
};

const openStoryPdf = (button) => {
    const modal = document.querySelector("[data-story-pdf-modal]");
    if (!modal || !button || button.disabled) {
        return;
    }
    modal.querySelector("[data-story-pdf-title]").textContent = button.dataset.storyPdfTitle || "개인 이야기";
    const frame = modal.querySelector("[data-story-pdf-frame]");
    frame.src = button.dataset.storyPdfSrc || "";
    modal.hidden = false;
    document.body.classList.add("story-pdf-open");
};

const closeStoryPdf = () => {
    const modal = document.querySelector("[data-story-pdf-modal]");
    if (!modal) {
        return;
    }
    modal.hidden = true;
    const frame = modal.querySelector("[data-story-pdf-frame]");
    if (frame) {
        frame.removeAttribute("src");
    }
    document.body.classList.remove("story-pdf-open");
};

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
    const stage = document.querySelector("[data-stage-hints]");
    if (!stage) {
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
    const groups = new Map();
    personalHints.forEach((hint) => {
        if (!groups.has(hint.playerCode)) {
            groups.set(hint.playerCode, {
                code: hint.playerCode,
                name: hint.playerName,
                self: hint.self,
                items: []
            });
        }
        groups.get(hint.playerCode).items.push(hint);
    });
    const bundleCards = [...groups.values()].map((group, index) => {
        const readableItems = group.items.filter((hint) => hint.self && hint.readable);
        const newCount = readableItems.filter((hint) => !seenHintIds.has(hint.id)).length;
        const testimonyCount = group.items.filter((hint) => hint.typeCode === "TESTIMONY").length;
        const evidenceCount = group.items.filter((hint) => hint.typeCode === "EVIDENCE").length;
        const openable = readableItems.length > 0;
        return `
            <button class="record-folder ${openable ? "is-readable" : "is-locked"} ${group.self ? "is-self" : ""}"
                    ${openable ? `data-hint-bundle="${escapeHtml(group.code)}"` : ""}
                    style="--token-index:${index}"
                    type="button">
                <span>${escapeHtml(group.name)} 기록</span>
                <strong>${testimonyCount} 증언 · ${evidenceCount} 물증</strong>
                <em>${openable ? "펼쳐볼 수 있음" : "봉인된 기록철"}</em>
                ${newCount > 0 ? `<b class="new-badge">새 단서 ${newCount}개</b>` : ""}
            </button>`;
    }).join("");
    const sharedItems = personalHints.filter((hint) => hint.shared);
    const sharedNewCount = sharedItems.filter((hint) => !seenHintIds.has(hint.id)).length;
    stage.innerHTML = `
        <div class="stage-hints-private" aria-label="캐릭터 기록철">
            ${bundleCards}
        </div>
        <aside class="stage-hints-shared" aria-label="전체 공개 단서">
            <button class="record-folder shared-folder ${sharedItems.length > 0 ? "is-readable is-shared" : "is-locked"}"
                    ${sharedItems.length > 0 ? "data-shared-bundle" : ""}
                    type="button">
                <span>전체 공개 단서</span>
                <strong>${sharedItems.length}개 기록</strong>
                <em>${sharedItems.length > 0 ? "모두 열람 가능" : "아직 없음"}</em>
                ${sharedNewCount > 0 ? `<b class="new-badge">새 단서 ${sharedNewCount}개</b>` : ""}
            </button>
        </aside>
    `;
};

const shareHint = async (hintId) => {
    const hint = personalHints.find((entry) => entry.id === hintId);
    if (!hint) {
        return;
    }
    if (hint.shared) {
        showToast("이미 전체 공개된 기록입니다.");
        return;
    }
    if (!window.confirm("전체 공개하면 모든 플레이어가 볼 수 있으며 되돌릴 수 없습니다. 공개할까요?")) {
        return;
    }
    const result = await postForm("/api/share/hint", {hintId});
    showToast(result.message);
    await renderHints();
    await renderPublicStatuses();
    await renderPublicRecords();
    await renderEvidence();
};

const openHintDetail = (hint) => {
    const panel = document.querySelector("[data-evidence-detail]");
    if (!hint || !panel) {
        return;
    }
    if (!hint.readable) {
        return;
    }
    rememberSeen("mysterySeenHints", seenHintIds, hint.id);
    panel.querySelector("[data-evidence-meta]").textContent = `${hint.roundLabel} · ${hint.type}`;
    panel.querySelector("[data-evidence-title]").textContent = hint.title;
    panel.querySelector("[data-evidence-short]").textContent = hint.shared ? "전체 공개된 기록입니다." : "아직 다른 플레이어에게 공개하지 않았습니다.";
    panel.querySelector("[data-evidence-keywords]").innerHTML = `<em>${escapeHtml(hint.type)}</em><em>${escapeHtml(hint.roundLabel)}</em>`;
    panel.querySelector("[data-evidence-body]").textContent = hint.body;
    const shareButton = panel.querySelector("[data-detail-share]");
    if (shareButton) {
        shareButton.hidden = !(hint.self && !hint.shared);
        shareButton.dataset.shareHint = hint.id;
    }
    panel.hidden = false;
};

const openHintBundle = (items, title, subtitle) => {
    const panel = document.querySelector("[data-evidence-detail]");
    if (!panel || items.length === 0) {
        return;
    }
    items.forEach((hint) => {
        if (hint.readable) {
            rememberSeen("mysterySeenHints", seenHintIds, hint.id);
        }
    });
    panel.querySelector("[data-evidence-meta]").textContent = subtitle;
    panel.querySelector("[data-evidence-title]").textContent = title;
    panel.querySelector("[data-evidence-short]").textContent = "기록철 안의 단서를 펼쳐 확인합니다.";
    panel.querySelector("[data-evidence-keywords]").innerHTML = `<em>증언</em><em>물증</em><em>공개 여부</em>`;
    panel.querySelector("[data-evidence-body]").innerHTML = items.map((hint) => `
        <article class="bundle-entry ${hint.shared ? "is-shared" : ""}">
            <span>${escapeHtml(hint.roundLabel)} · ${escapeHtml(hint.type)} · ${hint.shared ? "전체 공개" : "비공개"}</span>
            <h3>${escapeHtml(hint.title)}</h3>
            <p>${escapeHtml(hint.body)}</p>
            ${hint.self && !hint.shared ? `<button class="button ghost small" data-bundle-share="${escapeHtml(hint.id)}" type="button">전체 공개</button>` : ""}
        </article>
    `).join("");
    const shareButton = panel.querySelector("[data-detail-share]");
    if (shareButton) {
        shareButton.hidden = true;
        shareButton.removeAttribute("data-share-hint");
    }
    panel.hidden = false;
};

const renderStory = async () => {
    const locked = document.querySelector("[data-story-locked]");
    const content = document.querySelector("[data-story-content]");
    const actingLocked = document.querySelector("[data-acting-locked]");
    const actingContent = document.querySelector("[data-acting-content]");
    if (!actingLocked || !actingContent) {
        return;
    }
    const response = await fetch("/api/story", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const story = await response.json();
    if (actingLocked) {
        actingLocked.hidden = story.revealed;
    }
    if (actingContent) {
        actingContent.hidden = !story.revealed;
    }
    if (story.revealed) {
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
    document.querySelectorAll(".investigation-link").forEach((link) => {
        link.hidden = !state.investigationOpen;
    });
    document.querySelectorAll("[data-story-locked]").forEach((node) => {
        node.hidden = state.personalStoryRevealed;
    });
    document.querySelectorAll("[data-story-pdf-button]").forEach((button) => {
        button.disabled = !state.personalStoryRevealed;
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
    if (!event.target.closest("[data-evidence-menu]")) {
        closeEvidenceContextMenu();
    }

    const storyPdfButton = event.target.closest("[data-story-pdf-open]");
    if (storyPdfButton) {
        openStoryPdf(storyPdfButton);
        return;
    }

    if (event.target.closest("[data-story-pdf-close]")) {
        closeStoryPdf();
        return;
    }

    const bundleShareButton = event.target.closest("[data-bundle-share]");
    if (bundleShareButton) {
        await shareHint(bundleShareButton.dataset.bundleShare);
        const ownerCode = personalHints.find((hint) => hint.id === bundleShareButton.dataset.bundleShare)?.playerCode;
        if (ownerCode) {
            const ownItems = personalHints.filter((hint) => hint.playerCode === ownerCode && hint.self && hint.readable);
            openHintBundle(ownItems, `${ownItems[0]?.playerName || "내"} 기록`, "내 기록철");
        }
        return;
    }

    const detailShareButton = event.target.closest("[data-detail-share]");
    if (detailShareButton) {
        await shareHint(detailShareButton.dataset.shareHint);
        return;
    }

    const hintShareButton = event.target.closest("[data-share-hint]");
    if (hintShareButton) {
        await shareHint(hintShareButton.dataset.shareHint);
        return;
    }

    const hintToken = event.target.closest("[data-hint-token]");
    if (hintToken) {
        openHintDetail(personalHints.find((hint) => hint.id === hintToken.dataset.hintToken));
        return;
    }

    const bundleButton = event.target.closest("[data-hint-bundle]");
    if (bundleButton) {
        const items = personalHints.filter((hint) => hint.playerCode === bundleButton.dataset.hintBundle && hint.self && hint.readable);
        openHintBundle(items, `${items[0]?.playerName || "내"} 기록`, "내 기록철");
        return;
    }

    if (event.target.closest("[data-shared-bundle]")) {
        const items = personalHints.filter((hint) => hint.shared && hint.readable);
        openHintBundle(items, "전체 공개 단서", "공개된 기록철");
        return;
    }

    const sceneButton = event.target.closest("[data-scene-target]");
    if (sceneButton) {
        document.querySelectorAll("[data-scene-target]").forEach((button) => {
            button.classList.toggle("is-active", button === sceneButton);
        });
        document.querySelectorAll("[data-scene-document]").forEach((documentNode) => {
            documentNode.classList.toggle("is-active", documentNode.id === sceneButton.dataset.sceneTarget);
        });
        return;
    }

    const locationEnter = event.target.closest("[data-location-enter]");
    if (locationEnter) {
        const result = await postForm("/api/investigation/location/enter", {locationId: locationEnter.dataset.locationId});
        showToast(result.message || (result.success ? "조사를 시작합니다." : "입장할 수 없습니다."));
        if (result.success) {
            investigationState.activeLocationId = locationEnter.dataset.locationId;
        }
        await refreshInvestigation();
        return;
    }

    const locationLeave = event.target.closest("[data-location-leave]");
    if (locationLeave) {
        const ownPrivate = investigationState.privateEvidence.filter((item) => item.locationId === locationLeave.dataset.locationId);
        const ownPublic = investigationState.publicEvidence.filter((item) => item.locationId === locationLeave.dataset.locationId);
        if (ownPrivate.length && !ownPublic.length && !window.confirm("아직 공개한 증거가 없습니다. 이대로 나가면 다른 플레이어들은 이 장면에서 발견한 증거를 알 수 없습니다.\n\n그래도 나가시겠습니까?")) {
            return;
        }
        const result = await postForm("/api/investigation/location/leave", {locationId: locationLeave.dataset.locationId});
        showToast(result.message || "조사를 종료했습니다.");
        investigationState.activeLocationId = "";
        await refreshInvestigation();
        return;
    }

    const investigationEvidence = event.target.closest("[data-investigation-evidence]");
    if (investigationEvidence) {
        const detail = await fetchEvidenceDetail(investigationEvidence.dataset.evidenceId, true);
        if (detail) {
            openMemoryEvidenceDetail(detail);
            await refreshInvestigation();
        }
        return;
    }

    const privateEvidenceCard = event.target.closest("[data-private-evidence-card]");
    if (privateEvidenceCard) {
        const detail = await fetchEvidenceDetail(privateEvidenceCard.dataset.evidenceId, false);
        if (detail) {
            openMemoryEvidenceDetail(detail);
        }
        return;
    }

    const publicEvidenceCard = event.target.closest("[data-public-evidence-card]");
    if (publicEvidenceCard) {
        const detail = await fetchEvidenceDetail(publicEvidenceCard.dataset.evidenceId, false);
        if (detail) {
            openMemoryEvidenceDetail(detail);
        }
        return;
    }

    if (event.target.closest("[data-menu-publish]")) {
        openPublishConfirm(investigationState.menuEvidenceId);
        closeEvidenceContextMenu();
        return;
    }

    if (event.target.closest("[data-menu-detail]")) {
        const detail = await fetchEvidenceDetail(investigationState.menuEvidenceId, false);
        closeEvidenceContextMenu();
        if (detail) {
            openMemoryEvidenceDetail(detail);
        }
        return;
    }

    if (event.target.closest("[data-menu-close]")) {
        closeEvidenceContextMenu();
        return;
    }

    if (event.target.closest("[data-confirm-publish]")) {
        const result = await postForm("/api/investigation/evidence/publish", {evidenceId: investigationState.pendingPublishId});
        showToast(result.message || "증거를 공개했습니다.");
        closePublishConfirm();
        await refreshInvestigation();
        return;
    }

    if (event.target.closest("[data-confirm-cancel]")) {
        closePublishConfirm();
        return;
    }

    const memoryEvidence = event.target.closest("[data-memory-evidence]");
    if (memoryEvidence) {
        openMemoryEvidence(memoryEvidence);
        return;
    }

    if (event.target.closest("[data-memory-close]")) {
        closeMemoryEvidence();
        return;
    }

    const hintOpenButton = event.target.closest("[data-hint-open]");
    if (hintOpenButton) {
        openHintDetail(personalHints.find((hint) => hint.id === hintOpenButton.dataset.hintOpen));
        return;
    }

    const hintNote = event.target.closest("[data-hint-id]");
    if (hintNote) {
        openHintDetail(personalHints.find((hint) => hint.id === hintNote.dataset.hintId));
        return;
    }

    const panelButton = event.target.closest("[data-panel-target]");
    if (panelButton) {
        const target = document.getElementById(panelButton.dataset.panelTarget);
        if (target && target.id === "acting-panel") {
            await renderStory();
        }
        document.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.hidden = panel !== target;
        });
        if (target) {
            target.hidden = false;
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

document.addEventListener("contextmenu", (event) => {
    const card = event.target.closest("[data-private-evidence-card]");
    if (!card) {
        return;
    }
    event.preventDefault();
    openEvidenceContextMenu(card.dataset.evidenceId, event.clientX, event.clientY);
});

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        closeEvidenceContextMenu();
        closePublishConfirm();
        closeMemoryEvidence();
        closeStoryPdf();
    }
});

let longPressTimer = null;
document.addEventListener("pointerdown", (event) => {
    const card = event.target.closest("[data-private-evidence-card]");
    if (!card || event.pointerType === "mouse") {
        return;
    }
    longPressTimer = window.setTimeout(() => {
        openEvidenceContextMenu(card.dataset.evidenceId, event.clientX, event.clientY);
    }, 620);
});

document.addEventListener("pointerup", () => {
    window.clearTimeout(longPressTimer);
});

initStaticReader();
refreshInvestigation();
refreshState();
window.setInterval(refreshState, 1000);
window.setInterval(refreshInvestigation, 5000);
