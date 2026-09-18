-- =====================================================================
-- V10 — Dữ liệu tham chiếu tối thiểu để hệ thống chạy được ngay
-- Chỉ chứa dữ liệu CẤU HÌNH, không chứa từ vựng hay tài khoản thật.
-- Dùng ON CONFLICT DO NOTHING để chạy lại an toàn.
-- =====================================================================

-- ---------------------------------------------------------------------
-- roles — khớp constant/enums/RoleType.java, mở rộng cho CMS
-- ---------------------------------------------------------------------
INSERT INTO roles (code, name_vi, description, is_system) VALUES
    ('SYSTEM_ADMIN',   'Quản trị hệ thống', 'Toàn quyền, kể cả phân quyền và cấu hình', TRUE),
    ('CONTENT_EDITOR', 'Biên tập nội dung', 'Từ vựng, video, chủ đề, bài học, câu hỏi. Không đụng người dùng', TRUE),
    ('MODERATOR',      'Kiểm duyệt viên',   'Chỉ diễn đàn: duyệt, ẩn, xử lý báo cáo', TRUE),
    ('VSL_REVIEWER',   'Thẩm định viên VSL','Duyệt tính đúng đắn của ký hiệu và duyệt vai trò chuyên môn', TRUE),
    ('TEACHER',        'Giáo viên',         'Tài khoản giáo viên', TRUE),
    ('STUDENT',        'Người học',         'Vai trò mặc định khi đăng ký', TRUE)
ON CONFLICT (code) DO NOTHING;


INSERT INTO permissions (code, name_vi, module) VALUES
    ('sign:read',      'Xem từ vựng',            'DICTIONARY'),
    ('sign:write',     'Sửa từ vựng',            'DICTIONARY'),
    ('sign:import',    'Nhập từ vựng hàng loạt', 'DICTIONARY'),
    ('sign:review',    'Thẩm định ký hiệu',      'DICTIONARY'),
    ('video:ingest',   'Nạp video hàng loạt',    'DICTIONARY'),
    ('course:write',   'Quản lý khoá học',       'CATALOG'),
    ('quiz:write',     'Quản lý câu hỏi và đề',  'PRACTICE'),
    ('forum:moderate', 'Kiểm duyệt diễn đàn',    'FORUM'),
    ('user:read',      'Xem người dùng',         'USER'),
    ('user:write',     'Quản lý người dùng',     'USER'),
    ('user:verify_role','Duyệt vai trò chuyên môn','USER'),
    ('ai:threshold',   'Chỉnh ngưỡng chấm điểm', 'AI'),
    ('system:config',  'Cấu hình hệ thống',      'SYSTEM')
ON CONFLICT (code) DO NOTHING;


-- Gán quyền theo vai trò
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SYSTEM_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
    ('sign:read','sign:write','sign:import','video:ingest','course:write','quiz:write')
WHERE r.code = 'CONTENT_EDITOR'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
    ('forum:moderate','user:read')
WHERE r.code = 'MODERATOR'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
    ('sign:read','sign:review','user:verify_role','ai:threshold')
WHERE r.code = 'VSL_REVIEWER'
ON CONFLICT DO NOTHING;


-- ---------------------------------------------------------------------
-- verify_threshold_groups — TẦNG 3, phải có sẵn từ ngày đầu.
-- Không có bảng này thì ngày mở không chấm được từ nào, vì cả hiệu chỉnh
-- từ exemplar lẫn từ góp ý giáo viên đều chưa có dữ liệu.
--
-- Ký hiệu càng dài, càng nhiều tay thì khoảng cách DTW càng cộng dồn lớn,
-- nên ngưỡng phải nới theo. Đây là giá trị KHỞI ĐIỂM, hiệu chỉnh lại sau
-- khi đo trên dữ liệu thật.
-- ---------------------------------------------------------------------
INSERT INTO verify_threshold_groups (unit_type, hand_count, threshold, note) VALUES
    ('LETTER',   1, 0.1800, 'Chữ cái ngón tay, rất ngắn, dao động tự nhiên thấp'),
    ('LETTER',   2, 0.2200, NULL),
    ('NUMBER',   1, 0.1800, NULL),
    ('NUMBER',   2, 0.2200, NULL),
    ('WORD',     1, 0.2400, 'Từ đơn một tay'),
    ('WORD',     2, 0.3000, 'Từ đơn hai tay, nhiều landmark chuyển động hơn'),
    ('PHRASE',   1, 0.3400, NULL),
    ('PHRASE',   2, 0.3900, NULL),
    ('SENTENCE', 1, 0.4000, 'Câu dài, nhiều giai đoạn, dao động tự nhiên cao nhất'),
    ('SENTENCE', 2, 0.4500, NULL)
ON CONFLICT (unit_type, hand_count) DO NOTHING;


-- ---------------------------------------------------------------------
-- topics — 12 chủ đề khởi điểm theo tài liệu yêu cầu
-- ---------------------------------------------------------------------
INSERT INTO topics (slug, name_vi, description_vi, icon_name, category, display_order, is_published) VALUES
    ('bang-chu-cai',      'Bảng chữ cái',        'Chữ cái ngón tay và dấu thanh tiếng Việt', 'alphabet', 'SIMPLE_SIGN', 1,  TRUE),
    ('so-dem',            'Số đếm',              'Chữ số và cách đếm',                        'number',   'SIMPLE_SIGN', 2,  TRUE),
    ('gia-dinh',          'Gia đình',            'Các thành viên trong gia đình',             'family',   'SIMPLE_SIGN', 3,  TRUE),
    ('giao-tiep-hang-ngay','Giao tiếp hằng ngày','Chào hỏi, cảm ơn, xin lỗi',                 'chat',     'SITUATION',   4,  TRUE),
    ('thuc-pham',         'Thực phẩm',           'Món ăn, đồ uống',                           'food',     'SIMPLE_SIGN', 5,  TRUE),
    ('truong-hoc',        'Trường học',          'Lớp học, môn học, đồ dùng',                 'school',   'SIMPLE_SIGN', 6,  TRUE),
    ('cong-viec',         'Công việc',           'Nghề nghiệp và nơi làm việc',               'work',     'SIMPLE_SIGN', 7,  TRUE),
    ('cam-xuc',           'Cảm xúc',             'Trạng thái và cảm xúc',                     'emotion',  'SIMPLE_SIGN', 8,  TRUE),
    ('suc-khoe',          'Sức khoẻ',            'Cơ thể, bệnh tật, khám chữa bệnh',          'health',   'COMPLEX_SIGN',9,  TRUE),
    ('du-lich',           'Du lịch',             'Phương tiện, địa điểm, phương hướng',       'travel',   'COMPLEX_SIGN',10, TRUE),
    ('thoi-gian',         'Thời gian',           'Ngày, tháng, giờ giấc',                     'time',     'SIMPLE_SIGN', 11, TRUE),
    ('dia-danh',          'Địa danh',            'Tỉnh thành và quốc gia',                    'map',      'COMPLEX_SIGN',12, TRUE)
ON CONFLICT (slug) DO NOTHING;


-- ---------------------------------------------------------------------
-- forum_categories
-- ---------------------------------------------------------------------
INSERT INTO forum_categories (slug, name_vi, description_vi, icon_name, display_order) VALUES
    ('hoi-dap',      'Hỏi đáp',            'Đặt câu hỏi về ký hiệu và cách dùng', 'help',     1),
    ('luyen-tap',    'Luyện tập cùng nhau','Đăng video tự quay để mọi người góp ý','video',    2),
    ('chia-se',      'Chia sẻ',            'Kinh nghiệm học và câu chuyện cá nhân','share',    3),
    ('gop-y',        'Góp ý sản phẩm',     'Báo lỗi và đề xuất tính năng',         'feedback', 4)
ON CONFLICT (slug) DO NOTHING;


-- ---------------------------------------------------------------------
-- achievements
-- ---------------------------------------------------------------------
INSERT INTO achievements (code, name_vi, description_vi, icon_name, criteria_json, display_order) VALUES
    ('FIRST_LESSON',  'Bài học đầu tiên', 'Hoàn thành bài học đầu tiên',      'star',   '{"type":"lessons_completed","value":1}',  1),
    ('STREAK_3',      'Ba ngày liên tiếp','Học 3 ngày liên tiếp',             'flame',  '{"type":"streak","value":3}',            2),
    ('STREAK_7',      'Một tuần bền bỉ',  'Học 7 ngày liên tiếp',             'flame',  '{"type":"streak","value":7}',            3),
    ('STREAK_30',     'Một tháng kiên trì','Học 30 ngày liên tiếp',           'trophy', '{"type":"streak","value":30}',           4),
    ('WORDS_50',      '50 từ đầu tiên',   'Học thuộc 50 từ vựng',             'book',   '{"type":"signs_learned","value":50}',    5),
    ('WORDS_200',     '200 từ vựng',      'Học thuộc 200 từ vựng',            'book',   '{"type":"signs_learned","value":200}',   6),
    ('QUIZ_PERFECT',  'Điểm tuyệt đối',   'Đạt 100% một bài kiểm tra',        'medal',  '{"type":"quiz_perfect","value":1}',      7),
    ('FIRST_AI_CHECK','Lần đầu thử tay',  'Hoàn thành lần chấm ký hiệu đầu tiên','hand','{"type":"ai_checks","value":1}',         8)
ON CONFLICT (code) DO NOTHING;


-- ---------------------------------------------------------------------
-- notification_templates
--
-- Chữ hiển thị giữ tông trung tính, không trách móc. Sản phẩm này báo
-- "chưa đạt" rất nhiều lần với người đang học; giọng văn quyết định họ
-- có quay lại hôm sau hay không.
-- ---------------------------------------------------------------------
INSERT INTO notification_templates (code, type, channel, subject_vi, body_template) VALUES
    ('LESSON_REMINDER_EMAIL', 'LESSON_REMINDER', 'EMAIL', 'Bài học hôm nay đang đợi bạn',
     'Chào {{fullName}}, hôm nay bạn chưa hoàn thành bài học. Chỉ {{dailyGoalMinutes}} phút thôi nhé!'),
    ('LESSON_REMINDER_PUSH',  'LESSON_REMINDER', 'PUSH',  'Bài học hôm nay',
     'Bạn chưa học hôm nay. Dành {{dailyGoalMinutes}} phút nhé!'),
    ('STREAK_WARNING_PUSH',   'STREAK_WARNING',  'PUSH',  'Giữ chuỗi {{currentStreak}} ngày',
     'Chuỗi {{currentStreak}} ngày của bạn sẽ kết thúc lúc nửa đêm. Học một chút để giữ nhé!'),
    ('STREAK_LOST_INAPP',     'STREAK_LOST',     'IN_APP','Chuỗi học đã kết thúc',
     'Chuỗi {{previousStreak}} ngày đã dừng lại. Bắt đầu chuỗi mới hôm nay nhé.'),
    ('NEW_COURSE_INAPP',      'NEW_COURSE',      'IN_APP','Có bài học mới',
     'Chủ đề {{topicName}} vừa có bài học mới.'),
    ('ACHIEVEMENT_INAPP',     'ACHIEVEMENT',     'IN_APP','Bạn vừa đạt thành tích mới',
     'Chúc mừng! Bạn vừa nhận được "{{achievementName}}".'),
    ('FORUM_REPLY_INAPP',     'FORUM_REPLY',     'IN_APP','Có trả lời mới',
     '{{actorName}} vừa trả lời bài "{{postTitle}}".'),
    ('MODERATION_RESULT_INAPP','MODERATION_RESULT','IN_APP','Kết quả kiểm duyệt',
     'Nội dung của bạn {{decision}}. {{reason}}'),
    ('ROLE_VERIFIED_EMAIL',   'ROLE_VERIFIED',   'EMAIL', 'Vai trò chuyên môn đã được xác minh',
     'Chào {{fullName}}, vai trò {{vslRole}} của bạn đã được xác minh. Góp ý của bạn về chấm điểm giờ sẽ được ưu tiên.')
ON CONFLICT (type, channel) DO NOTHING;


-- ---------------------------------------------------------------------
-- system_settings
-- ---------------------------------------------------------------------
INSERT INTO system_settings (key, value, description) VALUES
    ('ai.check.level',            '"A"'::jsonb,   'Mức AI checking: A = chỉ luyện tập, B = tính điểm trong quiz'),
    ('ai.feedback.weight.verified','5'::jsonb,    'Trọng số góp ý của TEACHER / DEAF_NATIVE / INTERPRETER đã xác minh'),
    ('ai.feedback.weight.learner','1'::jsonb,     'Trọng số góp ý của người học — không dùng chỉnh ngưỡng, chỉ phát hiện bất thường'),
    ('ai.threshold.min_weight',   '10'::jsonb,    'Tổng trọng số tối thiểu trước khi cho phép cập nhật ngưỡng'),
    ('ai.threshold.max_shift_pct','15'::jsonb,    'Mỗi lần hiệu chỉnh dịch chuyển tối đa bao nhiêu phần trăm'),
    ('streak.freeze.monthly',     '2'::jsonb,     'Số băng cứu chuỗi cấp mỗi tháng'),
    ('streak.freeze.max',         '5'::jsonb,     'Số băng tối đa tích được'),
    ('streak.warning.min_days',   '3'::jsonb,     'Chỉ nhắc giữ chuỗi khi chuỗi đã đạt số ngày này'),
    ('forum.video.max_seconds',   '60'::jsonb,    'Thời lượng tối đa của video bình luận'),
    ('forum.video.max_bytes',     '104857600'::jsonb, 'Dung lượng tối đa khi tải video lên (100MB)'),
    ('forum.moderation.pre_check','true'::jsonb,  'Tiền kiểm mọi nội dung mới trước khi hiện công khai'),
    ('quiz.default_option_count', '4'::jsonb,     'Số đáp án mặc định của đề sinh tự động')
ON CONFLICT (key) DO NOTHING;
