const api = {
    list: () => fetch('/api/chats').then(r => r.json()),
    create: (provider) => fetch('/api/chats', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ provider })
    }).then(r => r.json()),
    get: (id) => fetch(`/api/chats/${id}`).then(r => r.json()),
    remove: (id) => fetch(`/api/chats/${id}`, { method: 'DELETE' }),
    send: (id, content) => fetch(`/api/chats/${id}/chatMessages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content })
    }).then(async r => {
        if (!r.ok) {
            const problem = await r.json().catch(() => ({}));
            throw new Error(problem.detail || 'Request failed');
        }
        return r.json();
    })
};

const learningApi = {
    topics: () => fetch('/api/learning-path/topics').then(r => r.json()),
    diagnose: (payload) => fetch('/api/learning-path/diagnose', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(async r => {
        if (!r.ok) {
            const problem = await r.json().catch(() => ({}));
            throw new Error(problem.detail || 'Learning path request failed');
        }
        return r.json();
    })
};

const els = {
    chatList: document.getElementById('chat-list'),
    messages: document.getElementById('chatMessages'),
    title: document.getElementById('chat-title'),
    input: document.getElementById('input'),
    send: document.getElementById('send'),
    form: document.getElementById('composer'),
    error: document.getElementById('error'),
    newChat: document.getElementById('new-chat'),
    newChatProvider: document.getElementById('new-chat-provider'),
    providerBadge: document.getElementById('provider-badge'),
    learningForm: document.getElementById('learning-form'),
    learnerGoal: document.getElementById('learner-goal'),
    learnerStruggles: document.getElementById('learner-struggles'),
    learningTopics: document.getElementById('learning-topics'),
    knowledgeArticles: document.getElementById('knowledge-articles'),
    autoSelectTopics: document.getElementById('auto-select-topics'),
    timeAvailable: document.getElementById('time-available'),
    learningProvider: document.getElementById('learning-provider'),
    diagnose: document.getElementById('diagnose'),
    learningError: document.getElementById('learning-error'),
    learningResults: document.getElementById('learning-results'),
    viewButtons: document.querySelectorAll('.mode-nav button'),
    views: document.querySelectorAll('.view')
};

let activeChatId = null;
let learningTopics = [];
const ignoredPromptTokens = new Set([
    'about', 'build', 'but', 'confused', 'get', 'have', 'into', 'learn', 'make',
    'need', 'the', 'this', 'want', 'with', 'work', 'understand'
]);
const requiredTopicSignals = {
    'spring-ai': new Set(['agent', 'ai', 'chat', 'model', 'ollama', 'prompt'])
};

function setError(message) {
    els.error.textContent = message || '';
}

function setComposerEnabled(enabled) {
    els.input.disabled = !enabled;
    els.send.disabled = !enabled;
    if (!enabled) {
        els.providerBadge.style.display = 'none';
    }
}

async function refreshChatList() {
    const chats = await api.list();
    els.chatList.innerHTML = '';
    chats.forEach(chat => els.chatList.appendChild(renderChatItem(chat)));
}

function renderChatItem(chat) {
    const item = document.createElement('div');
    item.className = 'chat-item' + (chat.id === activeChatId ? ' active' : '');
    item.onclick = () => openChat(chat.id);

    const title = document.createElement('span');
    title.className = 'title';
    title.textContent = chat.title;

    const badge = document.createElement('span');
    const prov = chat.provider || 'ollama';
    badge.className = `list-badge ${prov.toLowerCase()}`;
    badge.textContent = prov === 'openrouter' ? 'OpenRouter' : 'Ollama';

    const count = document.createElement('span');
    count.className = 'count';
    count.textContent = chat.messageCount;

    const del = document.createElement('button');
    del.className = 'del';
    del.textContent = 'Delete';
    del.onclick = (e) => { e.stopPropagation(); deleteChat(chat.id); };

    item.append(title, badge, count, del);
    return item;
}

function renderMessages(chat) {
    els.title.textContent = chat.title;
    els.messages.innerHTML = '';

    const prov = chat.provider || 'ollama';
    els.providerBadge.className = prov.toLowerCase();
    els.providerBadge.textContent = prov === 'openrouter' ? 'OpenRouter' : 'Ollama';
    els.providerBadge.style.display = 'inline-flex';

    if (!chat.chatMessages.length) {
        els.messages.innerHTML = '<div class="empty">Say hello to start the conversation.</div>';
        return;
    }
    chat.chatMessages.forEach(m => els.messages.appendChild(renderMessage(m)));
    els.messages.scrollTop = els.messages.scrollHeight;
}

function renderMessage(message) {
    const wrapper = document.createElement('div');
    wrapper.className = `msg ${message.role.toLowerCase()}`;
    const role = document.createElement('div');
    role.className = 'role';
    role.textContent = message.role;
    const body = document.createElement('div');
    body.className = 'formatted';
    body.appendChild(formatText(message.content));
    wrapper.append(role, body);
    return wrapper;
}

async function openChat(id) {
    setError('');
    activeChatId = id;
    const chat = await api.get(id);
    renderMessages(chat);
    setComposerEnabled(true);
    els.input.focus();
    await refreshChatList();
}

async function startNewChat() {
    setError('');
    const chat = await api.create(els.newChatProvider.value);
    await refreshChatList();
    await openChat(chat.id);
}

async function deleteChat(id) {
    setError('');
    await api.remove(id);
    if (id === activeChatId) {
        activeChatId = null;
        els.title.textContent = 'Select or start a chat';
        els.messages.innerHTML = '<div class="empty">No conversation selected.</div>';
        setComposerEnabled(false);
    }
    await refreshChatList();
}

async function submitMessage(content) {
    setError('');
    setComposerEnabled(false);
    appendPending(content);
    try {
        const chat = await api.send(activeChatId, content);
        renderMessages(chat);
        await refreshChatList();
    } catch (err) {
        setError(err.message);
        const chat = await api.get(activeChatId);
        renderMessages(chat);
    } finally {
        setComposerEnabled(true);
        els.input.focus();
    }
}

function appendPending(content) {
    els.messages.querySelector('.empty')?.remove();
    els.messages.appendChild(renderMessage({ role: 'USER', content }));
    const thinking = renderMessage({ role: 'ASSISTANT', content: '…' });
    thinking.classList.add('pending');
    els.messages.appendChild(thinking);
    els.messages.scrollTop = els.messages.scrollHeight;
}

function setLearningError(message) {
    els.learningError.textContent = message || '';
}

function setLearningEnabled(enabled) {
    els.diagnose.disabled = !enabled;
    els.autoSelectTopics.disabled = !enabled;
}

async function loadLearningTopics() {
    try {
        learningTopics = await learningApi.topics();
        els.learningTopics.innerHTML = '';
        learningTopics.forEach(topic => els.learningTopics.appendChild(renderTopicOption(topic, defaultTopicIds().has(topic.id))));
        renderKnowledgeArticles();
        updateArticleSelection();
    } catch (err) {
        setLearningError(err.message);
    }
}

function defaultTopicIds() {
    return new Set(['java-core', 'oop', 'spring-crm', 'spring-boot', 'testing']);
}

function renderTopicOption(topic, checked) {
    const label = document.createElement('label');
    label.className = 'topic-option';

    const input = document.createElement('input');
    input.type = 'checkbox';
    input.value = topic.id;
    input.checked = checked;
    input.addEventListener('change', updateArticleSelection);

    const text = document.createElement('span');
    text.className = 'topic-copy';

    const title = document.createElement('strong');
    title.textContent = topic.title;

    const summary = document.createElement('span');
    summary.className = 'topic-summary';
    summary.textContent = topic.summary;

    text.append(title, summary);
    label.append(input, text);
    return label;
}

function selectedLearningTopics() {
    return Array.from(els.learningTopics.querySelectorAll('input:checked'))
        .map(input => input.value);
}

function autoSelectTopicsFromPrompt() {
    const prompt = `${els.learnerGoal.value} ${els.learnerStruggles.value}`.trim();
    const ranked = prompt ? rankedTopicIds(prompt) : [];
    const selected = ranked.length ? ranked : Array.from(defaultTopicIds());
    const selectedSet = new Set(selected.slice(0, 5));

    els.learningTopics.querySelectorAll('input[type="checkbox"]').forEach(input => {
        input.checked = selectedSet.has(input.value);
    });

    updateArticleSelection();
    if (!prompt || !ranked.length) {
        setLearningError('No strong prompt match found. Default topics selected for now.');
    } else {
        setLearningError('');
    }
}

function rankedTopicIds(prompt) {
    const promptTokens = tokenize(prompt);
    const scoredTopics = learningTopics
        .map(topic => ({
            id: topic.id,
            score: scoreTopic(topic, promptTokens)
        }))
        .filter(topic => topic.score > 0)
        .sort((left, right) => right.score - left.score || left.id.localeCompare(right.id));

    if (!scoredTopics.length) {
        return [];
    }

    const bestScore = scoredTopics[0].score;
    const minimumUsefulScore = Math.max(6, bestScore * 0.35);
    return scoredTopics
        .filter(topic => topic.score >= minimumUsefulScore)
        .map(topic => topic.id);
}

function scoreTopic(topic, promptTokens) {
    const titleTokens = tokenize(topic.title);
    const summaryTokens = tokenize(topic.summary);
    const articleTokens = tokenize(topic.article);
    const requiredSignals = requiredTopicSignals[topic.id];

    if (requiredSignals && !hasAnyToken(promptTokens, requiredSignals)) {
        return 0;
    }

    let score = 0;

    promptTokens.forEach(token => {
        if (titleTokens.has(token)) {
            score += 8;
        }
        if (summaryTokens.has(token)) {
            score += 5;
        }
        if (articleTokens.has(token)) {
            score += 2;
        }
        combinedTokens(titleTokens, summaryTokens).forEach(topicToken => {
            if (isRelatedToken(token, topicToken)) {
                score += 1;
            }
        });
    });

    return score;
}

function tokenize(value) {
    return new Set(
        String(value || '')
            .toLowerCase()
            .split(/[^a-z0-9]+/)
            .map(normalizeToken)
            .filter(token => token.length > 2)
            .filter(token => !ignoredPromptTokens.has(token))
    );
}

function normalizeToken(token) {
    if (token === 'dtos') return 'dto';
    if (token.length > 4 && token.endsWith('ies')) return token.slice(0, -3) + 'y';
    if (token.length > 3 && token.endsWith('s')) return token.slice(0, -1);
    return token;
}

function hasAnyToken(tokens, expectedTokens) {
    for (const token of expectedTokens) {
        if (tokens.has(token)) {
            return true;
        }
    }
    return false;
}

function combinedTokens(...tokenSets) {
    const combined = new Set();
    tokenSets.forEach(tokenSet => {
        tokenSet.forEach(token => combined.add(token));
    });
    return combined;
}

function isRelatedToken(promptToken, topicToken) {
    return promptToken.length > 4
        && topicToken.length > 4
        && (topicToken.startsWith(promptToken) || promptToken.startsWith(topicToken));
}

async function submitLearningDiagnosis() {
    setLearningError('');
    els.learningResults.innerHTML = '<div class="empty">Running the diagnostic workflow...</div>';
    setLearningEnabled(false);
    try {
        const response = await learningApi.diagnose({
            learnerGoal: els.learnerGoal.value.trim(),
            struggles: els.learnerStruggles.value.trim(),
            topics: selectedLearningTopics(),
            timeAvailableMinutes: Number(els.timeAvailable.value),
            provider: els.learningProvider.value
        });
        renderLearningResults(response);
    } catch (err) {
        setLearningError(err.message);
        els.learningResults.innerHTML = '';
    } finally {
        setLearningEnabled(true);
    }
}

function renderLearningResults(response) {
    els.learningResults.innerHTML = '';
    els.learningResults.append(
        renderDiagnosisBlock(response.diagnosis),
        renderContextBlock(response.retrievedContext),
        renderPracticeBlock(response.practicePlan),
        renderTextBlock('Coach message', response.coachMessage),
        renderTraceBlock(response.agentTrace)
    );
}

function renderDiagnosisBlock(diagnosis) {
    const block = createResultBlock('Diagnosis');
    const summary = document.createElement('div');
    summary.className = 'formatted';
    summary.appendChild(formatText(`${diagnosis.summary}\n\nConfidence: ${diagnosis.confidenceScore}/100.`));
    const list = document.createElement('ul');
    diagnosis.weakSpots.forEach(weakSpot => {
        const item = document.createElement('li');
        item.appendChild(formatInlineText(weakSpot));
        list.appendChild(item);
    });
    block.append(summary, list);
    return block;
}

function renderContextBlock(context) {
    const block = createResultBlock('Retrieved context');
    const list = document.createElement('div');
    list.className = 'context-list';
    context.forEach(item => {
        const pill = document.createElement('span');
        pill.className = 'context-pill';
        pill.textContent = item.title;
        pill.title = item.guidance;
        list.appendChild(pill);
    });
    block.appendChild(list);
    return block;
}

function renderPracticeBlock(plan) {
    const block = createResultBlock(`Practice plan (${plan.timeBoxMinutes} min)`);
    const list = document.createElement('ol');
    plan.steps.forEach(step => {
        const item = document.createElement('li');
        const title = document.createElement('strong');
        title.textContent = `${step.title} (${step.durationMinutes} min)`;
        const instructions = document.createElement('div');
        instructions.className = 'formatted';
        instructions.appendChild(formatText(step.instructions));
        item.append(title, instructions);
        list.appendChild(item);
    });
    block.appendChild(list);
    return block;
}

function renderTextBlock(title, text) {
    const block = createResultBlock(title);
    const content = document.createElement('div');
    content.className = 'formatted';
    content.appendChild(formatText(text));
    block.appendChild(content);
    return block;
}

function renderTraceBlock(trace) {
    const block = createResultBlock('Agent trace');
    const list = document.createElement('ul');
    trace.forEach(entry => {
        const item = document.createElement('li');
        item.textContent = `${entry.agent}: ${entry.purpose}`;
        list.appendChild(item);
    });
    block.appendChild(list);
    return block;
}

function createResultBlock(title) {
    const block = document.createElement('section');
    block.className = 'result-block';
    const heading = document.createElement('h3');
    heading.textContent = title;
    block.appendChild(heading);
    return block;
}

function formatText(value) {
    const container = document.createElement('div');
    const text = normalizeText(value);
    if (!text) {
        container.appendChild(document.createTextNode(''));
        return container;
    }

    const lines = text.split('\n');
    let paragraph = [];
    let list = null;
    let codeBlock = null;

    function flushParagraph() {
        if (!paragraph.length) return;
        const paragraphEl = document.createElement('p');
        paragraphEl.appendChild(formatInlineText(paragraph.join(' ')));
        container.appendChild(paragraphEl);
        paragraph = [];
    }

    function flushList() {
        if (!list) return;
        container.appendChild(list.element);
        list = null;
    }

    function appendListItem(type, content) {
        flushParagraph();
        if (!list || list.type !== type) {
            flushList();
            list = { type, element: document.createElement(type) };
        }
        const item = document.createElement('li');
        item.appendChild(formatInlineText(content));
        list.element.appendChild(item);
    }

    for (const rawLine of lines) {
        const line = rawLine.trimEnd();
        const trimmed = line.trim();

        if (trimmed.startsWith('```')) {
            flushParagraph();
            flushList();
            if (codeBlock) {
                container.appendChild(codeBlock);
                codeBlock = null;
            } else {
                codeBlock = document.createElement('pre');
                codeBlock.appendChild(document.createElement('code'));
            }
            continue;
        }

        if (codeBlock) {
            const code = codeBlock.querySelector('code');
            code.textContent += (code.textContent ? '\n' : '') + rawLine;
            continue;
        }

        if (!trimmed) {
            flushParagraph();
            flushList();
            continue;
        }

        const heading = trimmed.match(/^#{1,4}\s+(.+)$/);
        if (heading) {
            flushParagraph();
            flushList();
            const headingEl = document.createElement('div');
            headingEl.className = 'formatted-heading';
            headingEl.appendChild(formatInlineText(heading[1]));
            container.appendChild(headingEl);
            continue;
        }

        const bullet = trimmed.match(/^[-*]\s+(.+)$/);
        if (bullet) {
            appendListItem('ul', bullet[1]);
            continue;
        }

        const numbered = trimmed.match(/^\d+[.)]\s+(.+)$/);
        if (numbered) {
            appendListItem('ol', numbered[1]);
            continue;
        }

        paragraph.push(trimmed);
    }

    if (codeBlock) {
        container.appendChild(codeBlock);
    }
    flushParagraph();
    flushList();
    return container;
}

function formatInlineText(value) {
    const fragment = document.createDocumentFragment();
    const text = normalizeText(value);
    const pattern = /(`[^`]+`|\*\*[^*]+\*\*)/g;
    let lastIndex = 0;
    let match;

    while ((match = pattern.exec(text)) !== null) {
        if (match.index > lastIndex) {
            fragment.appendChild(document.createTextNode(text.slice(lastIndex, match.index)));
        }

        const token = match[0];
        if (token.startsWith('`')) {
            const code = document.createElement('code');
            code.textContent = token.slice(1, -1);
            fragment.appendChild(code);
        } else {
            const strong = document.createElement('strong');
            strong.textContent = token.slice(2, -2);
            fragment.appendChild(strong);
        }
        lastIndex = pattern.lastIndex;
    }

    if (lastIndex < text.length) {
        fragment.appendChild(document.createTextNode(text.slice(lastIndex)));
    }

    return fragment;
}

function normalizeText(value) {
    return String(value || '')
        .replace(/\\n/g, '\n')
        .replace(/\r\n?/g, '\n')
        .trim();
}

function renderKnowledgeArticles() {
    els.knowledgeArticles.innerHTML = '';
    learningTopics.forEach(topic => {
        const card = document.createElement('article');
        card.className = 'article-card';
        card.dataset.topicId = topic.id;

        const title = document.createElement('h3');
        title.textContent = topic.title;

        const summary = document.createElement('p');
        summary.textContent = topic.summary;

        const details = document.createElement('details');
        const toggle = document.createElement('summary');
        toggle.textContent = 'Read article';
        const article = document.createElement('div');
        article.className = 'article-body';
        article.textContent = topic.article;

        details.append(toggle, article);
        card.append(title, summary, details);
        els.knowledgeArticles.appendChild(card);
    });
}

function updateArticleSelection() {
    const selected = new Set(selectedLearningTopics());
    els.knowledgeArticles.querySelectorAll('.article-card').forEach(card => {
        card.classList.toggle('selected', selected.has(card.dataset.topicId));
    });
}

function showView(viewId) {
    els.views.forEach(view => {
        view.classList.toggle('active', view.id === viewId);
    });
    els.viewButtons.forEach(button => {
        button.classList.toggle('active', button.dataset.view === viewId);
    });
}

els.newChat.onclick = startNewChat;

els.autoSelectTopics.addEventListener('click', autoSelectTopicsFromPrompt);

els.viewButtons.forEach(button => {
    button.addEventListener('click', () => showView(button.dataset.view));
});

els.learningForm.addEventListener('submit', (e) => {
    e.preventDefault();
    submitLearningDiagnosis();
});

els.form.addEventListener('submit', (e) => {
    e.preventDefault();
    const content = els.input.value.trim();
    if (!content || !activeChatId) return;
    els.input.value = '';
    submitMessage(content);
});

els.input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        els.form.requestSubmit();
    }
});

refreshChatList();
loadLearningTopics();
