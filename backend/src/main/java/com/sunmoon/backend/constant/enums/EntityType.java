package com.sunmoon.backend.constant.enums;

/**
 * Giá trị cho cột file_attachments.entity_type.
 *
 * Mỗi hằng số là TÊN BẢNG nghiệp vụ sở hữu tệp. Giữ đúng tên bảng để khi truy
 * vết một tệp mồ côi còn biết ngay phải tìm ở đâu.
 */
public interface EntityType {

    // Module: File Attachment
    String FILE_ATTACHMENT = "file_attachments";

    // Module: Từ điển VSL
    String TOPIC       = "topics";
    String SIGN        = "signs";
    String SIGN_VIDEO  = "sign_videos";
    String SIGN_STEP   = "sign_steps";

    // Module: Khoá học
    String COURSE      = "courses";
    String LESSON      = "lessons";

    // Module: Luyện tập
    String QUIZ            = "quizzes";
    String QUIZ_QUESTION   = "quiz_questions";

    // Module: Người dùng
    String USER        = "users";

    // Module: Diễn đàn và blog
    String FORUM_POST    = "forum_posts";
    String FORUM_COMMENT = "forum_comments";
    String BLOG_POST     = "blog_posts";
}
