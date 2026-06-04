const formatTime = (seconds) => {
    const safe = Math.max(0, Number(seconds) || 0);
    const minutes = Math.floor(safe / 60).toString().padStart(2, "0");
    const rest = Math.floor(safe % 60).toString().padStart(2, "0");
    return `${minutes}:${rest}`;
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
    const contentType = response.headers.get("content-type") || "";
    if (contentType.includes("application/json")) {
        return response.json();
    }
    return {success: response.ok};
};

const showToast = (message) => {
    if (!message) {
        return;
    }
    if (chapterOverlayActive) {
        pendingToasts.push(message);
        return;
    }
    let stack = document.querySelector("[data-toast-stack]");
    if (!stack) {
        stack = document.createElement("div");
        stack.className = "toast-stack";
        stack.dataset.toastStack = "";
        document.body.appendChild(stack);
    }
    const item = document.createElement("div");
    item.className = "toast-item";
    item.textContent = message;
    stack.appendChild(item);
    window.setTimeout(() => {
        item.classList.add("is-leaving");
        window.setTimeout(() => item.remove(), 420);
    }, 3200);
    const legacyToast = document.querySelector("[data-toast]");
    if (!legacyToast) {
        return;
    }
    legacyToast.hidden = true;
};

let lastPhase = null;
let lastNoticeSnapshot = null;
let lastChapterCueId = null;
let chapterCueInitialized = false;
let chapterOverlayActive = false;
let pendingToasts = [];

const flushPendingToasts = () => {
    const queued = [...pendingToasts];
    pendingToasts = [];
    queued.forEach((message, index) => {
        window.setTimeout(() => showToast(message), index * 420);
    });
};

const endingState = {
    source: "",
    page: 0,
    pages: [[]]
};

const actingReaderState = {
    source: "",
    page: 0,
    pages: [{title: "", paragraphs: []}]
};

const privateEvidencePublishState = {
    evidenceId: "",
    title: ""
};

const chunkParagraphs = (body, size = 3) => {
    const paragraphs = String(body ?? "")
        .split(/\n+/)
        .map((text) => text.trim())
        .filter(Boolean);
    const pages = [];
    for (let index = 0; index < paragraphs.length; index += size) {
        pages.push(paragraphs.slice(index, index + size));
    }
    return pages.length ? pages : [[]];
};

const splitReaderPages = (body) => {
    const pages = chunkParagraphs(body, 3).map((paragraphs) => ({title: "", paragraphs}));
    return pages.length ? pages : [{title: "", paragraphs: []}];
};

const updateProgressGuidance = (state) => {
    const guide = document.querySelector("[data-progress-guidance]");
    if (!guide) {
        return;
    }
    const title = guide.querySelector("[data-progress-title]");
    const lineOne = guide.querySelector("[data-progress-line-one]");
    const lineTwo = guide.querySelector("[data-progress-line-two]");
    const discussionPhases = ["FIRST_DISCUSSION", "SECOND_DISCUSSION"];
    let copy = ["현재 진행 안내", "저택의 밤이 시작되기를 기다리고 있습니다.", "관리자의 진행에 따라 다음 기록이 열립니다."];
    if (state.endingRevealed || state.phase === "ENDING") {
        copy = ["엔딩 진행 중", "진실이 공개되고 있습니다.", "마지막 기록을 확인하세요."];
    } else if (state.phase === "VOTE") {
        copy = ["투표 진행 중", "이제 범인이라고 생각하는 인물을 선택하세요.", "선택은 신중하게 결정해주세요."];
    } else if (discussionPhases.includes(state.phase)) {
        copy = ["토론 진행 중", "지금은 토론 중입니다.", "공개된 증거를 확인하고, 서로의 이야기를 비교해보세요."];
    } else if (Number(state.privateEvidenceCount || 0) > 0) {
        copy = ["비공개 단서 도착", "당신에게 새로운 비공개 단서가 도착했습니다.", "다른 사람에게 말할지, 끝까지 숨길지는 당신의 선택입니다."];
    } else if (Number(state.publicEvidenceCount || 0) > 0) {
        copy = ["공용 증거 공개", "새로운 증거가 증거판에 공개되었습니다.", "확인한 내용을 토론 때 함께 비교해보세요."];
    } else if (state.personalStoryRevealed) {
        copy = ["개인 이야기 공개", "지금은 각자의 이야기를 읽는 시간입니다.", "당신의 이야기와 연기 힌트를 확인하세요."];
    }
    if (title) {
        title.textContent = copy[0];
    }
    if (lineOne) {
        lineOne.textContent = copy[1];
    }
    if (lineTwo) {
        lineTwo.textContent = copy[2];
    }
};

const notifyStateChanges = (state) => {
    if (!document.body.classList.contains("scene-game")) {
        return;
    }
    const snapshot = {
        publicEvidenceCount: Number(state.publicEvidenceCount || 0),
        privateEvidenceCount: Number(state.privateEvidenceCount || 0),
        publishedPrivateEvidenceCount: Number(state.publishedPrivateEvidenceCount || 0)
    };
    if (!lastNoticeSnapshot) {
        lastNoticeSnapshot = snapshot;
        return;
    }
    if (snapshot.publicEvidenceCount > lastNoticeSnapshot.publicEvidenceCount) {
        showToast("새로운 증거가 공개되었습니다.");
    }
    if (snapshot.privateEvidenceCount > lastNoticeSnapshot.privateEvidenceCount) {
        showToast("당신에게 비공개 단서가 도착했습니다.");
    }
    if (snapshot.publishedPrivateEvidenceCount > lastNoticeSnapshot.publishedPrivateEvidenceCount) {
        showToast("새로운 단서가 공개되었습니다.");
    }
    lastNoticeSnapshot = snapshot;
};

const ensureChapterOverlay = () => {
    let overlay = document.querySelector("[data-chapter-overlay]");
    if (overlay) {
        return overlay;
    }
    overlay = document.createElement("section");
    overlay.className = "chapter-overlay";
    overlay.dataset.chapterOverlay = "";
    overlay.hidden = true;
    overlay.innerHTML = `
        <article class="chapter-page">
            <p class="eyebrow" data-chapter-title></p>
            <div class="chapter-lines" data-chapter-lines></div>
        </article>
        <button class="chapter-skip" type="button" data-chapter-skip>건너뛰기</button>
    `;
    document.body.appendChild(overlay);
    overlay.querySelector("[data-chapter-skip]")?.addEventListener("click", () => finishChapterOverlay(overlay));
    return overlay;
};

const finishChapterOverlay = (overlay) => {
    if (!overlay || overlay.hidden) {
        return;
    }
    window.clearTimeout(finishChapterOverlay.timeout);
    overlay.classList.add("is-leaving");
    window.setTimeout(() => {
        overlay.hidden = true;
        overlay.classList.remove("is-visible", "is-leaving", "is-ending");
        chapterOverlayActive = false;
        flushPendingToasts();
    }, 720);
};

const showChapterOverlay = (cue) => {
    if (!document.body.classList.contains("scene-game") || !cue?.id) {
        return;
    }
    const overlay = ensureChapterOverlay();
    const title = overlay.querySelector("[data-chapter-title]");
    const lines = overlay.querySelector("[data-chapter-lines]");
    if (title) {
        title.textContent = cue.title || "";
    }
    if (lines) {
        lines.innerHTML = (cue.lines || []).map((line) => `<p>${escapeHtml(line)}</p>`).join("");
    }
    chapterOverlayActive = true;
    overlay.hidden = false;
    overlay.classList.toggle("is-ending", String(cue.id).includes("ENDING"));
    overlay.classList.remove("is-leaving");
    window.requestAnimationFrame(() => overlay.classList.add("is-visible"));
    window.clearTimeout(finishChapterOverlay.timeout);
    finishChapterOverlay.timeout = window.setTimeout(
        () => finishChapterOverlay(overlay),
        Math.max(3000, Number(cue.durationMs) || 3800)
    );
};

const handleChapterCue = (cue) => {
    if (!document.body.classList.contains("scene-game")) {
        return;
    }
    if (!cue?.id) {
        lastChapterCueId = null;
        chapterCueInitialized = true;
        return;
    }
    if (!chapterCueInitialized) {
        lastChapterCueId = cue.id;
        chapterCueInitialized = true;
        return;
    }
    if (cue.id !== lastChapterCueId) {
        lastChapterCueId = cue.id;
        showChapterOverlay(cue);
    }
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
    const finish = panel.querySelector("[data-ending-finish]");
    const page = endingState.pages[endingState.page] || [];
    if (body) {
        body.classList.remove("is-turning");
        window.requestAnimationFrame(() => body.classList.add("is-turning"));
        body.innerHTML = page.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("");
    }
    if (indicator) {
        indicator.textContent = `${endingState.page + 1} / ${endingState.pages.length}`;
    }
    if (prev) {
        prev.disabled = endingState.page === 0;
    }
    if (next) {
        const lastPage = endingState.page >= endingState.pages.length - 1;
        next.disabled = lastPage;
        next.textContent = lastPage ? "마지막" : "다음";
    }
    if (finish) {
        finish.hidden = endingState.page < endingState.pages.length - 1;
    }
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
    if (title) {
        title.textContent = state.endingTitle || "토끼가 남긴 마지막 시간";
    }
    if (endingState.source !== state.endingBody) {
        endingState.source = state.endingBody || "";
        endingState.page = 0;
        endingState.pages = chunkParagraphs(endingState.source, 2);
        paintEndingPage();
    }
};

const paintActingPage = () => {
    const panel = document.querySelector("[data-acting-content]");
    const controls = document.querySelector("[data-acting-controls]");
    if (!panel) {
        return;
    }
    const paragraphs = actingReaderState.pages.flatMap((page) => page.paragraphs || []);
    panel.hidden = false;
    panel.innerHTML = paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("");
    if (controls) {
        controls.hidden = true;
    }
};

const renderActingHint = async () => {
    const locked = document.querySelector("[data-acting-locked]");
    const content = document.querySelector("[data-acting-content]");
    const controls = document.querySelector("[data-acting-controls]");
    if (!locked && !content) {
        return;
    }
    const response = await fetch("/api/story", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const story = await response.json();
    if (!story.revealed) {
        if (locked) {
            locked.hidden = false;
        }
        if (content) {
            content.hidden = true;
        }
        if (controls) {
            controls.hidden = true;
        }
        return;
    }
    if (locked) {
        locked.hidden = true;
    }
    const actingHint = String(story.actingHint || "아직 별도의 연기 힌트가 없습니다.")
        .replace(/^연기\s*힌트\s*/u, "")
        .trim();
    if (actingReaderState.source !== actingHint) {
        actingReaderState.source = actingHint;
        actingReaderState.page = 0;
        actingReaderState.pages = [{title: "", paragraphs: chunkParagraphs(actingReaderState.source, 99).flatMap((page) => page)}];
    }
    paintActingPage();
};

const openStoryPdf = (button) => {
    const modal = document.querySelector("[data-story-pdf-modal]");
    if (!modal || !button || button.disabled) {
        showToast("아직 공개되지 않았습니다.");
        return;
    }
    const title = modal.querySelector("[data-story-pdf-title]");
    const frame = modal.querySelector("[data-story-pdf-frame]");
    if (title) {
        title.textContent = button.dataset.storyPdfTitle || "개인 이야기";
    }
    if (frame) {
        frame.src = button.dataset.storyPdfSrc || "";
    }
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

const openMemoryEvidence = (button) => {
    const modal = document.querySelector("[data-memory-modal]");
    if (!modal || !button) {
        return;
    }
    const title = modal.querySelector("[data-memory-title]");
    const location = modal.querySelector("[data-memory-location]");
    const publicDescription = modal.querySelector("[data-memory-public-description]");
    const memoryColumns = modal.querySelector(".memory-columns");
    const privateSection = modal.querySelector("[data-memory-private-section]");
    const privateInterpretation = modal.querySelector("[data-memory-private-interpretation]");
    const publishedBy = modal.querySelector("[data-memory-published-by]");
    const publishButton = modal.querySelector("[data-memory-publish]");
    if (title) {
        title.textContent = button.dataset.memoryTitle || "증거";
    }
    if (location) {
        location.textContent = button.dataset.memoryLocation || "";
    }
    if (publicDescription) {
        publicDescription.innerHTML = paragraphsHtml(button.dataset.memoryPublicDescription || button.dataset.memoryDescription || "");
    }
    const privateText = button.dataset.memoryPrivateInterpretation || "";
    if (memoryColumns) {
        memoryColumns.classList.toggle("has-private-detail", Boolean(privateText));
    }
    if (privateSection) {
        privateSection.hidden = !privateText;
    }
    if (privateInterpretation) {
        privateInterpretation.innerHTML = paragraphsHtml(privateText);
    }
    if (publishedBy) {
        const name = button.dataset.memoryPublishedBy || "";
        publishedBy.hidden = !name;
        publishedBy.textContent = name ? `공개자: ${name}` : "";
    }
    if (publishButton) {
        const publishable = button.dataset.memoryPublishable === "true";
        publishButton.hidden = !publishable;
        publishButton.dataset.evidenceId = button.dataset.memoryId || "";
        publishButton.dataset.evidenceTitle = button.dataset.memoryTitle || "";
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

const openPrivateEvidenceConfirm = (evidenceId, title) => {
    const modal = document.querySelector("[data-private-publish-confirm]");
    if (!modal) {
        return;
    }
    privateEvidencePublishState.evidenceId = evidenceId || "";
    privateEvidencePublishState.title = title || "비공개 단서";
    const titleNode = modal.querySelector("[data-private-publish-title]");
    if (titleNode) {
        titleNode.textContent = `[${privateEvidencePublishState.title}] 단서를 모두에게 공개하시겠습니까?`;
    }
    modal.hidden = false;
    document.body.classList.add("memory-modal-open");
};
const closePrivateEvidenceConfirm = () => {
    const modal = document.querySelector("[data-private-publish-confirm]");
    if (modal) {
        modal.hidden = true;
    }
    privateEvidencePublishState.evidenceId = "";
    privateEvidencePublishState.title = "";
};

const renderEvidenceCard = (evidence, mode, index = 0) => {
    const published = mode === "published-private";
    const privateMode = mode === "private";
    const publishable = privateMode && evidence.publishable;
    const label = published
        ? `공개자: ${escapeHtml(evidence.publishedByName || "")}`
        : (privateMode ? (evidence.status === "PUBLIC" ? "상태: 모두에게 공개됨" : "상태: 나만 알고 있음") : "공용 증거");
    const privateAttrs = privateMode ? `
        data-memory-id="${escapeHtml(evidence.id)}"
        data-memory-private-interpretation="${escapeHtml(evidence.privateInterpretation || "")}"
        data-memory-publishable="${publishable ? "true" : "false"}"
    ` : "";
    const publishedAttrs = published ? `data-memory-published-by="${escapeHtml(evidence.publishedByName || "")}"` : "";
    const action = privateMode ? `
        <div class="evidence-card-actions">
            <button class="button danger small" type="button" data-private-evidence-publish
                    data-evidence-id="${escapeHtml(evidence.id)}"
                    data-evidence-title="${escapeHtml(evidence.title)}"
                    ${publishable ? "" : "disabled"}>
                ${publishable ? "모두에게 공개하기" : "공개 완료"}
            </button>
        </div>
    ` : "";
    return `
        <article class="simple-evidence-card evidence-slot slot-${index + 1} ${privateMode ? "is-private" : ""} ${published ? "is-published-private" : ""}">
            <button class="evidence-card-open" type="button" data-memory-evidence
                    data-memory-mode="${mode}"
                    data-memory-title="${escapeHtml(evidence.title)}"
                    data-memory-location="${escapeHtml(evidence.locationText)}"
                    data-memory-public-description="${escapeHtml(evidence.publicDescription || evidence.description || "")}"
                    ${privateAttrs}
                    ${publishedAttrs}>
                <span>${label}</span>
                <strong>${escapeHtml(evidence.title)}</strong>
                <em>${escapeHtml(evidence.locationText)}</em>
                <small>확인하기</small>
            </button>
            ${action}
        </article>
    `;
};
const renderEvidenceList = (selector, items, mode, emptyMessage) => {
    const container = document.querySelector(selector);
    if (!container) {
        return;
    }
    const section = container.closest(".evidence-board-section");
    let empty = section ? section.querySelector(".empty-evidence-note") : null;
    if (!empty && section) {
        empty = document.createElement("p");
        empty.className = "empty-evidence-note";
        section.insertBefore(empty, container);
    }
    if (empty) {
        empty.textContent = emptyMessage;
        empty.hidden = items.length > 0;
    }
    if (section && section.classList.contains("game-frame")) {
        section.classList.toggle("is-empty", items.length === 0);
    }
    container.innerHTML = items.map((item, index) => renderEvidenceCard(item, mode, index)).join("");
};

const updateVoteSelectedName = (form) => {
    const voteForm = form || document.querySelector("[data-vote]");
    if (!voteForm) {
        return;
    }
    const selectedName = voteForm.querySelector("[data-vote-selected-name]");
    const checked = voteForm.querySelector("input[name='suspectCode']:checked");
    const label = checked ? checked.closest(".vote-target") : null;
    const name = label ? label.querySelector("strong") : null;
    if (selectedName && name) {
        selectedName.textContent = name.textContent.trim();
    }
};

const refreshEvidenceBoard = async () => {
    if (!document.querySelector("[data-evidence-board]")) {
        return;
    }
    const response = await fetch("/api/evidence-board", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const board = await response.json();
    renderEvidenceList("[data-public-evidence-list]", board.publicEvidence || [], "public", "아직 공개된 증거가 없습니다.");
    renderEvidenceList("[data-private-evidence-list]", board.privateEvidence || [], "private", "아직 지급된 비공개 단서가 없습니다.");
    renderEvidenceList("[data-published-private-evidence-list]", board.publishedPrivateEvidence || [], "published-private", "아직 공개된 비공개 단서가 없습니다.");
};

const publishPrivateEvidence = async () => {
    const evidenceId = privateEvidencePublishState.evidenceId;
    if (!evidenceId) {
        return;
    }
    const result = await postForm("/api/evidence/private/publish", {evidenceId});
    showToast(result.message || "단서 공개를 처리했습니다.");
    closePrivateEvidenceConfirm();
    closeMemoryEvidence();
    await refreshEvidenceBoard();
};

const refreshState = async () => {
    const response = await fetch("/api/state", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    const state = await response.json();
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
    document.querySelectorAll("[data-story-pdf-button]").forEach((button) => {
        button.disabled = !state.personalStoryRevealed;
    });
    document.querySelectorAll("[data-vote]").forEach((node) => {
        node.hidden = state.phase !== "VOTE";
        updateVoteSelectedName(node);
    });
    document.body.dataset.phase = state.phase;
    updateProgressGuidance(state);
    handleChapterCue(state.chapterCue);
    notifyStateChanges(state);
    renderEnding(state);
};

document.addEventListener("submit", async (event) => {
    const voteForm = event.target.closest("[data-vote]");
    if (!voteForm) {
        return;
    }
    event.preventDefault();
    const result = await postForm("/api/vote", {
        suspectCode: new FormData(voteForm).get("suspectCode")
    });
    showToast(result.message || "단서 공개를 처리했습니다.");
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
        const resetModal = document.querySelector("[data-reset-modal]");
        if (resetModal) {
            resetModal.hidden = true;
        }
        window.scrollTo(scrollX, scrollY);
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
        }
    }
});

document.addEventListener("click", async (event) => {
    if (event.target.closest("[data-reset-open]")) {
        const modal = document.querySelector("[data-reset-modal]");
        if (modal) {
            modal.hidden = false;
            modal.querySelector("[data-reset-code]")?.focus();
        }
        return;
    }

    if (event.target.closest("[data-reset-cancel]")) {
        const modal = document.querySelector("[data-reset-modal]");
        if (modal) {
            modal.hidden = true;
            const input = modal.querySelector("[data-reset-code]");
            const submit = modal.querySelector("[data-reset-submit]");
            if (input) {
                input.value = "";
            }
            if (submit) {
                submit.disabled = true;
            }
        }
        return;
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

    const memoryEvidence = event.target.closest("[data-memory-evidence]");
    if (memoryEvidence) {
        openMemoryEvidence(memoryEvidence);
        return;
    }

    if (event.target.closest("[data-memory-close]")) {
        closeMemoryEvidence();
        return;
    }

    const memoryPublishButton = event.target.closest("[data-memory-publish]");
    if (memoryPublishButton) {
        openPrivateEvidenceConfirm(memoryPublishButton.dataset.evidenceId, memoryPublishButton.dataset.evidenceTitle);
        return;
    }

    const privatePublishButton = event.target.closest("[data-private-evidence-publish]");
    if (privatePublishButton && !privatePublishButton.disabled) {
        openPrivateEvidenceConfirm(privatePublishButton.dataset.evidenceId, privatePublishButton.dataset.evidenceTitle);
        return;
    }

    if (event.target.closest("[data-private-publish-confirm-button]")) {
        await publishPrivateEvidence();
        return;
    }

    if (event.target.closest("[data-private-publish-cancel]")) {
        closePrivateEvidenceConfirm();
        return;
    }

    const panelButton = event.target.closest("[data-panel-target]");
    if (panelButton) {
        const target = document.getElementById(panelButton.dataset.panelTarget);
        document.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.hidden = panel !== target;
        });
        if (target) {
            target.hidden = false;
            if (target.id === "acting-panel") {
                await renderActingHint();
            }
        }
        return;
    }

    if (event.target.closest("[data-panel-close]")) {
        document.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.hidden = true;
        });
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

document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
        closeMemoryEvidence();
        closeStoryPdf();
        closePrivateEvidenceConfirm();
        const resetModal = document.querySelector("[data-reset-modal]");
        if (resetModal) {
            resetModal.hidden = true;
        }
    }
});

document.addEventListener("input", (event) => {
    const input = event.target.closest("[data-reset-code]");
    if (!input) {
        return;
    }
    const submit = document.querySelector("[data-reset-submit]");
    if (submit) {
        submit.disabled = input.value !== "RESET";
    }
});

document.addEventListener("change", (event) => {
    const voteInput = event.target.closest("[data-vote] input[name='suspectCode']");
    if (voteInput) {
        updateVoteSelectedName(voteInput.closest("[data-vote]"));
    }
});

updateVoteSelectedName();
refreshState();
refreshEvidenceBoard();
window.setInterval(refreshState, 1000);
window.setInterval(refreshEvidenceBoard, 4000);



