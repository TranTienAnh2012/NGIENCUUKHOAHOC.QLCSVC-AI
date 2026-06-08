/**
 * AI Chatbot Widget - JavaScript Logic
 * Handles chatbot UI interactions and Flask AI API integration
 */

(function () {
    'use strict';

    // Configuration
    const CONFIG = {
        API_URL: 'http://localhost:5000/api/ai/chatbot',
        TYPING_DELAY: 1000, // ms
        AUTO_SCROLL_DELAY: 100 // ms
    };

    // Suggested questions based on user role
    const SUGGESTED_QUESTIONS = {
        ADMIN: [
            'Có bao nhiêu thiết bị cần bảo trì?',
            'Thống kê báo hỏng trong tháng này',
            'Hướng dẫn quản lý người dùng',
            'Cách tạo báo cáo hệ thống'
        ],
        GIAO_VIEN: [
            'Làm thế nào để mượn thiết bị?',
            'Cách báo hỏng thiết bị',
            'Xem lịch sử mượn trả của tôi',
            'Quy trình trả thiết bị'
        ]
    };

    // DOM Elements
    let toggleBtn, container, messagesContainer, typingIndicator, chatForm, chatInput, sendBtn, suggestionsContainer;

    // State
    let isOpen = false;
    let conversationHistory = [];
    let userRole = 'ADMIN';
    let userId = 'anonymous';
    let userName = '';
    let userEmail = '';
    let sessionId = '';

    /**
     * Initialize chatbot
     */
    function init() {
        // Get DOM elements
        toggleBtn = document.getElementById('chatbot-toggle-btn');
        container = document.getElementById('chatbot-container');
        messagesContainer = document.getElementById('chatbot-messages');
        typingIndicator = document.getElementById('chatbot-typing');
        chatForm = document.getElementById('chatbot-form');
        chatInput = document.getElementById('chatbot-input');
        sendBtn = chatForm.querySelector('.chatbot-send-btn');
        suggestionsContainer = document.querySelector('.suggestions-chips');

        if (!toggleBtn || !container) {
            console.error('Chatbot elements not found');
            return;
        }

        // Get user role: ưu tiên window.CHATBOT_USER_ROLE (inject từ layout server-side)
        // Fallback về data-role attribute của suggestions-chips
        if (window.CHATBOT_USER_ROLE) {
            userRole = window.CHATBOT_USER_ROLE;
        } else {
            const roleAttr = suggestionsContainer?.getAttribute('data-role');
            if (roleAttr) {
                userRole = roleAttr;
            }
        }

        // Get userId and userName from widget data attributes (injected by Thymeleaf)
        const widgetEl = document.getElementById('ai-chatbot-widget');
        if (widgetEl) {
            if (widgetEl.getAttribute('data-user-id')) {
                userId = widgetEl.getAttribute('data-user-id');
            }
            if (widgetEl.getAttribute('data-user-name')) {
                userName = widgetEl.getAttribute('data-user-name');
            }
            if (widgetEl.getAttribute('data-user-email')) {
                userEmail = widgetEl.getAttribute('data-user-email');
            }
        }

        // Generate or restore sessionId (unique per browser tab session)
        sessionId = sessionStorage.getItem('chatbot_session_id_' + userId);
        if (!sessionId) {{
            sessionId = 'sess-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
            sessionStorage.setItem('chatbot_session_id_' + userId, sessionId);
        }}

        // Setup event listeners
        setupEventListeners();

        // Load suggested questions
        loadSuggestedQuestions();

        // Initialize Lucide icons
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }

        // Load conversation history from sessionStorage
        loadConversationHistory();

        // Setup resizable container
        setupResizer();
    }

    /**
     * Setup event listeners
     */
    function setupEventListeners() {
        // Toggle chatbot
        toggleBtn.addEventListener('click', toggleChatbot);

        // Minimize button
        const minimizeBtn = document.getElementById('chatbot-minimize-btn');
        if (minimizeBtn) {
            minimizeBtn.addEventListener('click', toggleChatbot);
        }

        // Form submit
        chatForm.addEventListener('submit', handleFormSubmit);

        // Close on Escape key
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && isOpen) {
                toggleChatbot();
            }
        });
    }

    /**
     * Toggle chatbot open/close
     */
    function toggleChatbot() {
        isOpen = !isOpen;
        toggleBtn.classList.toggle('active', isOpen);
        container.classList.toggle('active', isOpen);

        if (isOpen) {
            chatInput.focus();
            scrollToBottom();
        }
    }

    /**
     * Load suggested questions based on user role
     */
    function loadSuggestedQuestions() {
        if (!suggestionsContainer) return;

        const questions = SUGGESTED_QUESTIONS[userRole] || SUGGESTED_QUESTIONS.ADMIN;

        suggestionsContainer.innerHTML = questions.map(question =>
            `<button class="suggestion-chip" data-question="${question}">${question}</button>`
        ).join('');

        // Add click listeners to suggestion chips
        suggestionsContainer.querySelectorAll('.suggestion-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const question = chip.getAttribute('data-question');
                chatInput.value = question;
                handleFormSubmit(new Event('submit'));
            });
        });
    }

    /**
     * Handle form submit
     */
    async function handleFormSubmit(e) {
        e.preventDefault();

        const message = chatInput.value.trim();
        if (!message) return;

        // Add user message to chat
        addMessage(message, 'user');

        // Clear input
        chatInput.value = '';

        // Disable input while processing
        setInputState(false);

        // Show typing indicator
        showTypingIndicator();

        try {
            // Send message to AI API
            const response = await sendMessageToAI(message);

            // Hide typing indicator
            hideTypingIndicator();

            // Add AI response to chat
            addMessage(response, 'ai');

            // Save to conversation history
            conversationHistory.push(
                { role: 'user', content: message },
                { role: 'ai', content: response }
            );
            saveConversationHistory();

        } catch (error) {
            console.error('Error sending message:', error);
            hideTypingIndicator();
            
            let displayError = error.message || '';
            if (displayError.includes('Failed to fetch') || displayError.includes('fetch') || displayError.includes('NetworkError')) {
                displayError = 'Không thể kết nối đến AI API. Vui lòng kiểm tra xem Flask AI API có đang chạy không.';
            }
            addMessage(displayError, 'ai', true);
        } finally {
            setInputState(true);
            chatInput.focus();
        }
    }

    /**
     * Send message to Flask AI API
     */
    async function sendMessageToAI(message) {
        const response = await fetch(CONFIG.API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message: message,
                user_id: userId,
                session_id: sessionId,
                context: {
                    user_role: userRole,
                    user_name: userName,
                    user_email: userEmail,
                    conversation_history: conversationHistory.slice(-6)
                }
            })
        });

        if (!response.ok) {
            let errorMessage = `Lỗi từ máy chủ (Mã: ${response.status})`;
            try {
                const errData = await response.json();
                if (errData && errData.error) {
                    errorMessage = errData.error;
                }
            } catch (jsonErr) {
                errorMessage = `Lỗi kết nối máy chủ (Mã: ${response.status})`;
            }
            throw new Error(errorMessage);
        }

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || 'Unknown error');
        }

        return data.response;
    }

    /**
     * Add message to chat
     */
    function addMessage(content, role, isError = false) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `chatbot-message ${role}-message fade-in`;

        const avatarIcon = role === 'user' ? 'user' : 'bot';
        const time = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

        messageDiv.innerHTML = `
            <div class="message-avatar">
                <i data-lucide="${avatarIcon}"></i>
            </div>
            <div class="message-content">
                <div class="message-bubble ${isError ? 'error' : ''}">
                    ${formatMessage(content)}
                </div>
                <span class="message-time">${time}</span>
            </div>
        `;

        // Insert before typing indicator or at the end
        if (typingIndicator && typingIndicator.parentNode === messagesContainer) {
            messagesContainer.insertBefore(messageDiv, typingIndicator);
        } else {
            messagesContainer.appendChild(messageDiv);
        }

        // Initialize Lucide icons for new message
        if (typeof lucide !== 'undefined') {
            lucide.createIcons();
        }

        scrollToBottom();
    }

    /**
     * Format message content (convert line breaks, headers, and bullet lists cleanly)
     */
    function formatMessage(content) {
        // Prevent XSS but keep styling
        let escaped = content
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

        // Clean up newlines between list numbers and list titles/content
        escaped = escaped.replace(/(\n|^)(\d+\.)\s*\n\s*\*\*/g, '$1$2 **');
        escaped = escaped.replace(/(\n|^)(\d+\.)\s*\n\s*([^\n\d\s#\-*•])/g, '$1$2 $3');

        // 1. Convert Markdown Images: ![alt](url)
        escaped = escaped.replace(/!\[(.*?)\]\((.*?)\)/g, '<img src="$2" alt="$1" class="chatbot-image" onerror="this.style.display=\'none\'">');

        // 2. Convert Markdown Links: [text](url)
        escaped = escaped.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" class="chatbot-link" target="_self">$1</a>');

        // 3. Convert **bold** to <strong>
        escaped = escaped.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // 4. Convert *italic* to <em> (only when not part of bullet points)
        escaped = escaped.replace(/(?<!\*)\*(?!\s)(.*?)(?!\s)\*(?!\*)/g, '<em>$1</em>');

        // 5. Process line-by-line for bullet lists and headers
        let lines = escaped.split('\n');
        let inList = false;
        let resultLines = [];

        for (let line of lines) {
            let trimmed = line.trim();

            // Strip raw/rogue markdown markers on their own lines
            if (trimmed === '*' || trimmed === '-' || trimmed === '_') {
                continue;
            }

            // Headers
            if (trimmed.startsWith('### ')) {
                if (inList) { inList = false; resultLines.push('</ul>'); }
                resultLines.push(`<h4>${trimmed.substring(4).trim()}</h4>`);
                continue;
            }
            if (trimmed.startsWith('## ')) {
                if (inList) { inList = false; resultLines.push('</ul>'); }
                resultLines.push(`<h3>${trimmed.substring(3).trim()}</h3>`);
                continue;
            }
            if (trimmed.startsWith('# ')) {
                if (inList) { inList = false; resultLines.push('</ul>'); }
                resultLines.push(`<h2>${trimmed.substring(2).trim()}</h2>`);
                continue;
            }

            // Bullet points
            if (trimmed.startsWith('* ') || trimmed.startsWith('- ') || trimmed.startsWith('• ')) {
                let listContent = trimmed.substring(2).trim();
                if (!inList) {
                    inList = true;
                    resultLines.push('<ul>');
                }
                resultLines.push(`<li>${listContent}</li>`);
            } else {
                if (inList) {
                    inList = false;
                    resultLines.push('</ul>');
                }
                resultLines.push(line);
            }
        }
        if (inList) {
            resultLines.push('</ul>');
        }

        let processed = resultLines.join('\n');

        // Convert remaining newlines to breaks
        processed = processed.replace(/\n/g, '<br>');
        
        // Clean up excess breaks around block elements
        processed = processed.replace(/<br><ul/g, '<ul').replace(/<\/ul><br>/g, '</ul>');
        processed = processed.replace(/<\/li><br>/g, '</li>').replace(/<br><li>/g, '<li>');
        processed = processed.replace(/<ul><br>/g, '<ul>').replace(/<\/ul><br>/g, '</ul>');

        return `<div class="message-text">${processed}</div>`;
    }

    /**
     * Show typing indicator
     */
    function showTypingIndicator() {
        if (typingIndicator) {
            typingIndicator.style.display = 'flex';
            scrollToBottom();
        }
    }

    /**
     * Hide typing indicator
     */
    function hideTypingIndicator() {
        if (typingIndicator) {
            setTimeout(() => {
                typingIndicator.style.display = 'none';
            }, CONFIG.TYPING_DELAY);
        }
    }

    /**
     * Set input state (enabled/disabled)
     */
    function setInputState(enabled) {
        chatInput.disabled = !enabled;
        sendBtn.disabled = !enabled;
    }

    /**
     * Scroll to bottom of messages
     */
    function scrollToBottom() {
        setTimeout(() => {
            if (messagesContainer) {
                messagesContainer.scrollTop = messagesContainer.scrollHeight;
            }
        }, CONFIG.AUTO_SCROLL_DELAY);
    }

    /**
     * Save conversation history to sessionStorage
     */
    function saveConversationHistory() {{
        try {{
            sessionStorage.setItem('chatbot_history_' + userId, JSON.stringify(conversationHistory));
        }} catch (e) {{
            console.error('Error saving conversation history:', e);
        }}
    }}

    /**
     * Load conversation history from sessionStorage
     */
    function loadConversationHistory() {{
        try {{
            const saved = sessionStorage.getItem('chatbot_history_' + userId);
            if (saved) {{
                conversationHistory = JSON.parse(saved);

                // Restore messages to UI
                conversationHistory.forEach(msg => {{
                    if (msg.role === 'user' || msg.role === 'ai') {{
                        addMessage(msg.content, msg.role);
                    }}
                }});
            }}
        }} catch (e) {{
            console.error('Error loading conversation history:', e);
        }}
    }}

    /**
     * Setup resizable container with handles and persistent settings
     */
    function setupResizer() {
        const container = document.getElementById('chatbot-container');
        if (!container) return;

        // Create resize handles
        const topHandle = document.createElement('div');
        topHandle.className = 'chat-resize-handle chat-resize-n';
        
        const leftHandle = document.createElement('div');
        leftHandle.className = 'chat-resize-handle chat-resize-w';
        
        const topLeftHandle = document.createElement('div');
        topLeftHandle.className = 'chat-resize-handle chat-resize-nw';

        container.appendChild(topHandle);
        container.appendChild(leftHandle);
        container.appendChild(topLeftHandle);

        let startX, startY, startWidth, startHeight;

        function doDrag(e) {
            const clientX = e.clientX || (e.touches && e.touches[0].clientX);
            const clientY = e.clientY || (e.touches && e.touches[0].clientY);
            
            if (e.target.classList.contains('chat-resize-w') || e.target.classList.contains('chat-resize-nw')) {
                const newWidth = startWidth + (startX - clientX);
                if (newWidth > 320 && newWidth < window.innerWidth - 40) {
                    container.style.width = newWidth + 'px';
                }
            }
            if (e.target.classList.contains('chat-resize-n') || e.target.classList.contains('chat-resize-nw')) {
                const newHeight = startHeight + (startY - clientY);
                if (newHeight > 350 && newHeight < window.innerHeight - 120) {
                    container.style.height = newHeight + 'px';
                }
            }
        }

        function stopDrag() {
            document.removeEventListener('mousemove', doDrag);
            document.removeEventListener('mouseup', stopDrag);
            document.removeEventListener('touchmove', doDrag);
            document.removeEventListener('touchend', stopDrag);
            
            // Persist the size
            localStorage.setItem('chatbot_pref_width', container.style.width);
            localStorage.setItem('chatbot_pref_height', container.style.height);
        }

        function initDrag(e) {
            if (e.type === 'mousedown' && e.button !== 0) return;
            
            startX = e.clientX || (e.touches && e.touches[0].clientX);
            startY = e.clientY || (e.touches && e.touches[0].clientY);
            
            startWidth = parseInt(document.defaultView.getComputedStyle(container).width, 10);
            startHeight = parseInt(document.defaultView.getComputedStyle(container).height, 10);

            document.addEventListener('mousemove', doDrag);
            document.addEventListener('mouseup', stopDrag);
            document.addEventListener('touchmove', doDrag);
            document.addEventListener('touchend', stopDrag);
            
            e.preventDefault();
        }

        topHandle.addEventListener('mousedown', initDrag);
        leftHandle.addEventListener('mousedown', initDrag);
        topLeftHandle.addEventListener('mousedown', initDrag);
        
        topHandle.addEventListener('touchstart', initDrag);
        leftHandle.addEventListener('touchstart', initDrag);
        topLeftHandle.addEventListener('touchstart', initDrag);

        // Restore persisted size if available
        const prefWidth = localStorage.getItem('chatbot_pref_width');
        const prefHeight = localStorage.getItem('chatbot_pref_height');
        if (prefWidth) container.style.width = prefWidth;
        if (prefHeight) container.style.height = prefHeight;
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Expose global function to open chatbot programmatically
    window.openAiChatbot = function(message) {
        if (!isOpen) {
            toggleChatbot();
        }
        if (message) {
            chatInput.value = message;
            // Optionally, we could auto-submit here: chatForm.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
        }
    };

})();
