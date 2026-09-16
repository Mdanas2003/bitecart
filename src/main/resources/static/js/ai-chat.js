(() => {
    const chat = document.querySelector('[data-ai-chat]');
    if (!chat) return;

    const toggle = chat.querySelector('[data-chat-toggle]');
    const panel = chat.querySelector('[data-chat-panel]');
    const close = chat.querySelector('[data-chat-close]');
    const form = chat.querySelector('form');
    const input = chat.querySelector('textarea');
    const messages = chat.querySelector('[data-chat-messages]');
    const token = chat.dataset.csrfToken;
    const header = chat.dataset.csrfHeader || 'X-CSRF-TOKEN';

    const append = (text, type) => {
        const item = document.createElement('p');
        item.className = `ai-chat-message ai-chat-message--${type}`;
        item.textContent = text;
        messages.append(item);
        messages.scrollTop = messages.scrollHeight;
    };
    const setOpen = open => {
        panel.hidden = !open;
        toggle.setAttribute('aria-expanded', String(open));
        if (open) input.focus();
    };

    toggle.addEventListener('click', () => setOpen(panel.hidden));
    close.addEventListener('click', () => setOpen(false));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const message = input.value.trim();
        if (!message) return;
        append(message, 'user');
        input.value = '';
        input.disabled = true;
        const submit = form.querySelector('button[type="submit"]');
        submit.disabled = true;
        try {
            const response = await fetch('/api/chat/messages', {
                method: 'POST',
                headers: {'Content-Type': 'application/json', [header]: token},
                body: JSON.stringify({message})
            });
            const body = await response.json();
            append(body.message || 'BiteCart Assistant is temporarily unavailable. Please try again later.', 'assistant');
        } catch (error) {
            append('BiteCart Assistant is temporarily unavailable. Please try again later.', 'assistant');
        } finally {
            input.disabled = false;
            submit.disabled = false;
            input.focus();
        }
    });
})();
