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

const storyReaderState = {
    source: "",
    page: 0,
    pages: []
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
    const list = document.querySelector("[data-hint-list]");
    if (!list) {
        return;
    }
    const response = await fetch("/api/hints", {headers: {"Accept": "application/json"}});
    if (!response.ok) {
        return;
    }
    personalHints = await response.json();
    const renderGroup = (title, typeCode) => {
        const items = personalHints.filter((hint) => hint.typeCode === typeCode);
        return `
            <section class="hint-group">
                <h3>${title}</h3>
                ${items.map((hint) => `
                    <article class="hint-note compact" data-hint-id="${escapeHtml(hint.id)}">
                        <button class="hint-open" data-hint-open="${escapeHtml(hint.id)}" type="button">
                            <span>${escapeHtml(hint.roundLabel)} · ${escapeHtml(hint.type)}</span>
                            <strong>${escapeHtml(hint.title)}</strong>
                        </button>
                        <button class="button ghost small share-button" data-share-hint="${escapeHtml(hint.id)}" ${hint.shared ? "disabled" : ""} type="button">
                            ${hint.shared ? "공개됨" : "이 힌트 공개"}
                        </button>
                    </article>
                `).join("") || `<p class="empty-hints">아직 받은 ${title}이 없습니다.</p>`}
            </section>
        `;
    };
    list.innerHTML = renderGroup("증언", "TESTIMONY") + renderGroup("물증", "EVIDENCE");
    document.querySelectorAll("[data-empty-hints]").forEach((node) => {
        node.hidden = personalHints.length > 0;
    });
};

const openHintDetail = (hint) => {
    const panel = document.querySelector("[data-evidence-detail]");
    if (!hint || !panel) {
        return;
    }
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
    if (story.revealed) {
        const source = JSON.stringify([story.personalStory, story.secret, story.actingHint]);
        if (storyReaderState.source !== source) {
            storyReaderState.source = source;
            storyReaderState.pages = splitReaderPages([
                {title: "개인 이야기", body: story.personalStory},
                {title: "비밀 정보", body: story.secret},
                {title: "연기 힌트", body: story.actingHint}
            ]);
            storyReaderState.page = Math.min(storyReaderState.page, storyReaderState.pages.length - 1);
        }
        paintStoryPage();
    } else {
        document.querySelector("[data-story-controls]")?.setAttribute("hidden", "");
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
    list.innerHTML = publicEvidence.map((item) => `
        <button class="evidence-card evidence-${escapeHtml(item.visual)}" data-evidence-id="${escapeHtml(item.id)}" type="button">
            <span class="evidence-mark">${escapeHtml(evidenceIcon(item.visual))}</span>
            <strong>${escapeHtml(item.name)}</strong>
            <small>${escapeHtml(item.revealRound)} · ${escapeHtml(item.category)}</small>
            <p>${escapeHtml(item.shortDescription)}</p>
            <span class="evidence-tags">${(item.keywords || []).map((keyword) => `<em>${escapeHtml(keyword)}</em>`).join("")}</span>
        </button>
    `).join("");
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
    document.querySelectorAll("[data-stage-floorplan]").forEach((node) => {
        node.hidden = ["WAITING", "PROLOGUE", "PRIVATE_STORY"].includes(state.phase);
    });
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

document.addEventListener("click", (event) => {
    const hintShareButton = event.target.closest("[data-share-hint]");
    if (hintShareButton) {
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
        document.querySelectorAll("[data-panel]").forEach((panel) => {
            panel.hidden = panel !== target;
        });
        if (target) {
            target.hidden = false;
            if (target.id === "story-panel") {
                paintStoryPage();
            }
        }
        return;
    }

    const collapseButton = event.target.closest("[data-collapse-target]");
    if (collapseButton) {
        const board = collapseButton.closest(".collapsible-board");
        if (board) {
            board.classList.toggle("is-collapsed");
            collapseButton.textContent = board.classList.contains("is-collapsed") ? "펼치기" : "줄이기";
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
            panel.querySelector("[data-evidence-meta]").textContent = `${item.revealRound} · ${item.category}`;
            panel.querySelector("[data-evidence-title]").textContent = item.name;
            panel.querySelector("[data-evidence-short]").textContent = item.shortDescription;
            panel.querySelector("[data-evidence-keywords]").innerHTML = (item.keywords || []).map((keyword) => `<em>${escapeHtml(keyword)}</em>`).join("");
            panel.querySelector("[data-evidence-body]").textContent = item.detail;
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
