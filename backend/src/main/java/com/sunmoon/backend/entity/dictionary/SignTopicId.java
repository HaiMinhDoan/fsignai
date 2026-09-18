package com.sunmoon.backend.entity.dictionary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SignTopicId implements Serializable {

    @Column(name = "sign_id")
    private UUID signId;

    @Column(name = "topic_id")
    private UUID topicId;
}
