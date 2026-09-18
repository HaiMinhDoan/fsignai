package com.sunmoon.backend.entity.forum;

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
public class ForumCommentMediaId implements Serializable {

    @Column(name = "comment_id")
    private UUID commentId;

    @Column(name = "media_id")
    private UUID mediaId;
}
