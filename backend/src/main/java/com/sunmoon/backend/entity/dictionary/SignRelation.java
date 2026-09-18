package com.sunmoon.backend.entity.dictionary;

import com.sunmoon.backend.constant.enums.SignRelationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

// EASILY_CONFUSED phục vụ hai việc: cảnh báo người học, và sinh đáp án nhiễu
// chất lượng cao cho quiz. Quiz với đáp án nhiễu ngẫu nhiên quá dễ và không
// đo được năng lực thật.
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sign_relations")
public class SignRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sign_id", nullable = false)
    private Sign sign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_sign_id", nullable = false)
    private Sign relatedSign;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'RELATED'")
    @Column(name = "relation_type", nullable = false, length = 30)
    @Builder.Default
    private SignRelationType relationType = SignRelationType.RELATED;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
