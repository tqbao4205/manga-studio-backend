package com.mangaflow.studio.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * ── WebSocketConfig ──
 * Cấu hình WebSocket với STOMP + SockJS cho real-time communication.
 * <p>
 * 📌 STOMP: Giao thức messaging layer trên WebSocket — giống như HTTP nhưng cho real-time.
 *    Frontend subscribe vào 1 topic → backend push message đến topic đó → frontend nhận được.
 * <p>
 * 📌 SockJS: Fallback khi trình duyệt không hỗ trợ WebSocket (chuyển sang HTTP polling).
 * <p>
 * ══════════════════════════════════════════════════════════════════
 *  Cách hoạt động:
 * ══════════════════════════════════════════════════════════════════
 *  Frontend:  kết nối tới http://localhost:8080/ws (SockJS)
 *             subscribe tới /topic/user/{userId}
 *  Backend:   gọi messagingTemplate.convertAndSend("/topic/user/5", payload)
 *             → tất cả frontend đang subscribe topic đó đều nhận được payload
 * <p>
 * 📌 @Configuration: Đánh dấu class này là Spring Configuration.
 * 📌 @EnableWebSocketMessageBroker: Bật WebSocket + STOMP message broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * configureMessageBroker: Cấu hình STOMP message broker.
     * <p>
     * 📌 enableSimpleBroker("/topic"):
     *    - Định nghĩa prefix cho các topic mà backend có thể push message đến.
     *    - Frontend subscribe vào /topic/** để nhận message.
     *    - VD: /topic/user/5 — topic riêng cho user có id = 5.
     * <p>
     * 📌 setApplicationDestinationPrefixes("/app"):
     *    - Prefix cho các message từ frontend gửi lên backend (nếu cần).
     *    - Ở file này, chúng ta chỉ dùng 1 chiều (backend → frontend),
     *      nên prefix này không dùng đến, nhưng vẫn cần set để STOMP hoạt động.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Cho phép backend push message tới các topic có prefix /topic
        registry.enableSimpleBroker("/topic");

        // Prefix cho message từ frontend gửi lên backend (nếu có)
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * registerStompEndpoints: Đăng ký endpoint WebSocket cho frontend kết nối.
     * <p>
     * 📌 addEndpoint("/ws"):
     *    - Đường dẫn mà frontend sẽ dùng để thiết lập kết nối WebSocket.
     *    - VD: frontend gọi new SockJS("http://localhost:8080/ws")
     * <p>
     * 📌 setAllowedOriginPatterns("*"):
     *    - Cho phép kết nối từ bất kỳ origin nào (trong dev).
     *    - Khi deploy production, nên giới hạn lại domain cụ thể.
     * <p>
     * 📌 withSockJS():
     *    - Bật SockJS fallback — nếu WebSocket không hoạt động,
     *      tự động chuyển sang HTTP polling hoặc streaming.
     *    - Bắt buộc phải có để tương thích với @stomp/stompjs + sockjs-client.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")                 // endpoint: ws://host/ws
                .setAllowedOriginPatterns("*")      // cho phép mọi origin (dev)
                .withSockJS();                      // bật SockJS fallback
    }
}
