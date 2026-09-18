package com.sunmoon.backend.entity.dictionary;

import jakarta.persistence.*;
import lombok.*;

// Một từ thuộc nhiều chủ đề
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sign_topics")
public class SignTopic {

    @EmbeddedId
    private SignTopicId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("signId")
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
}
