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
    let userRole = 'ADMIN'; // Default, will be set from HTML

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

        // Get user role from data attribute
        const roleAttr = suggestionsContainer?.getAttribute('data-role');
        if (roleAttr) {
            userRole = roleAttr;
        }

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
            addMessage('Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau hoặc kiểm tra xem Flask AI API có đang chạy không.', 'ai', true);
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
                context: {
                    user_role: userRole,
                    conversation_history: conversationHistory.slice(-6) // Last 3 exchanges
                }
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
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
     * Format message content (convert line breaks, etc.)
     */
    function formatMessage(content) {
        // Convert line breaks to <br>
        content = content.replace(/\n/g, '<br>');

        // Convert **bold** to <strong>
        content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

        // Convert bullet points
        content = content.replace(/^- (.+)$/gm, '<li>$1</li>');
        if (content.includes('<li>')) {
            content = content.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');
        }

        return `<p>${content}</p>`;
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
    function saveConversationHistory() {
        try {
            sessionStorage.setItem('chatbot_history', JSON.stringify(conversationHistory));
        } catch (e) {
            console.error('Error saving conversation history:', e);
        }
    }

    /**
     * Load conversation history from sessionStorage
     */
    function loadConversationHistory() {
        try {
            const saved = sessionStorage.getItem('chatbot_history');
            if (saved) {
                conversationHistory = JSON.parse(saved);

                // Restore messages to UI
                conversationHistory.forEach(msg => {
                    if (msg.role === 'user' || msg.role === 'ai') {
                        addMessage(msg.content, msg.role);
                    }
                });
            }
        } catch (e) {
            console.error('Error loading conversation history:', e);
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
