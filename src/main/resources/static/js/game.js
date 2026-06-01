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
    const toast = document.querySelector("[data-toast]");
    if (!toast) {
        if (message) {
            alert(message);
        }
        return;
    }
    toast.textContent = message;
    toast.hidden = false;
    window.clearTimeout(showToast.timeout);
    showToast.timeout = window.setTimeout(() => {
        toast.hidden = true;
    }, 3500);
};

let lastPhase = null;

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

const showPhaseCurtain = (phaseLabel, phaseKey) => {
    const curtain = document.querySelector("[data-phase-curtain]");
    if (!curtain) {
        return;
    }
    const lines = {
        WAITING: ["대기실", "저택의 촛불이 아직 흔들리고 있습니다."],
        STORY_REVEAL: ["개인 이야기 공개", "각자의 오래된 기억이 봉인을 풉니다."],
        FIRST_DISCUSSION: ["토론 시작", "같은 장면을 보았는데도 모두의 이야기는 다릅니다."],
        SECOND_DISCUSSION: ["두 번째 토론", "의심은 사람보다 기억을 먼저 향합니다."],
        FINAL_DISCUSSION: ["최종 토론", "당신들이 본 것은 정말 현실이었을까요."],
        VOTE: ["투표 직전", "괴물은 누구인가."],
        ENDING: ["마지막 진실", "토끼가 남긴 시간이 열립니다."]
    };
    const selected = lines[phaseKey] || [phaseLabel || "다음 막", "장면이 천천히 바뀝니다."];
    const title = curtain.querySelector("p");
    const note = curtain.querySelector("span");
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
        }, 700);
    }, 1700);
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
        body.innerHTML = page.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("");
    }
    if (indicator) {
        indicator.textContent = `${endingState.page + 1} / ${endingState.pages.length}`;
    }
    if (prev) {
        prev.disabled = endingState.page === 0;
    }
    if (next) {
        next.disabled = endingState.page >= endingState.pages.length - 1;
        next.textContent = endingState.page >= endingState.pages.length - 1 ? "마지막" : "다음";
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
        endingState.pages = chunkParagraphs(endingState.source, 3);
        paintEndingPage();
    }
};

const paintActingPage = () => {
    const panel = document.querySelector("[data-acting-content]");
    const controls = document.querySelector("[data-acting-controls]");
    if (!panel || !controls) {
        return;
    }
    const page = actingReaderState.pages[actingReaderState.page] || actingReaderState.pages[0];
    panel.hidden = false;
    panel.innerHTML = `
        ${page.title ? `<h3>${escapeHtml(page.title)}</h3>` : ""}
        ${page.paragraphs.map((paragraph) => `<p>${escapeHtml(paragraph)}</p>`).join("")}
    `;
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
        next.textContent = actingReaderState.page >= actingReaderState.pages.length - 1 ? "마지막" : "다음";
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
    if (actingReaderState.source !== story.actingHint) {
        actingReaderState.source = story.actingHint || "아직 별도의 연기 힌트가 없습니다.";
        actingReaderState.page = 0;
        actingReaderState.pages = splitReaderPages(actingReaderState.source);
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

const renderEvidenceCard = (evidence, mode) => {
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
        <article class="simple-evidence-card ${privateMode ? "is-private" : ""} ${published ? "is-published-private" : ""}">
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
    container.innerHTML = items.map((item) => renderEvidenceCard(item, mode)).join("");
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
    document.querySelectorAll("[data-story-pdf-button]").forEach((button) => {
        button.disabled = !state.personalStoryRevealed;
    });
    document.querySelectorAll("[data-vote]").forEach((node) => {
        node.hidden = state.phase !== "VOTE";
    });
    document.body.dataset.phase = state.phase;
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
        window.scrollTo(scrollX, scrollY);
    } finally {
        if (submitButton) {
            submitButton.disabled = false;
        }
    }
});

document.addEventListener("click", async (event) => {
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
    }
});

refreshState();
refreshEvidenceBoard();
window.setInterval(refreshState, 1000);
window.setInterval(refreshEvidenceBoard, 4000);



