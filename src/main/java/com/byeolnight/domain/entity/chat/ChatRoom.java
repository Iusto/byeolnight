//
//package com.byeolnight.domain.entity.chat;
//
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "chat_rooms")
//public class ChatRoom {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false)
//    private String name;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private Type type;
//
//    private LocalDateTime createdAt = LocalDateTime.now();
//
//    public enum Type {
//        GROUP, PRIVATE
//    }
//
//    // Getters and Setters omitted for brevity
//}
