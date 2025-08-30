package com.example.demo.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FcmService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendMessage(String token, String title, String body, Map<String, String> data) {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .putData("title", title)
                .putData("body", body);

        if (data != null) {
            builder.putAllData(data);
        }

        Message message = builder.build();

        try {
            String response = firebaseMessaging.send(message);
            System.out.println("✅ FCM 전송 성공: " + response);
        } catch (FirebaseMessagingException e) {
            System.err.println("❌ FCM 전송 실패: " + e.getMessage());
        }
    }

}
