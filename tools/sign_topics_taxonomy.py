# -*- coding: utf-8 -*-
"""
Bảng phân loại chủ đề cho kho từ vựng VSL (nguồn MOET_QIPEDC, 3322 từ).

Mỗi mục: (slug, name_vi, description_vi, icon_name, category, display_order, keywords)
keywords: danh sách từ khoá KHÔNG DẤU, khớp theo TỪ NGUYÊN VẸN (word boundary)
trên chuỗi đã bỏ dấu của word_vi + gloss (KHÔNG dùng description_vi — mô tả là
câu văn đầy đủ, chứa quá nhiều từ thông dụng và gây khớp sai tràn lan).

Thứ tự trong danh sách chính là THỨ TỰ ƯU TIÊN chọn primary_topic khi một từ
khớp nhiều chủ đề — chủ đề càng đặc thù càng đứng trước.

LƯU Ý về việc bỏ dấu: nhiều cặp từ tiếng Việt khác dấu nhưng giống hệt nhau
sau khi bỏ dấu (vd: "chuồng" và "chuông" đều thành "chuong"; "cạnh" và "canh"
đều thành "canh"). Từ khoá NGẮN và CHUNG CHUNG rất dễ dính phải những cặp này
— vì vậy ưu tiên cụm 2-3 từ cụ thể hơn là một từ đơn lẻ mơ hồ.
"""

EXISTING_SLUGS = {
    'bang-chu-cai', 'so-dem', 'gia-dinh', 'giao-tiep-hang-ngay', 'thuc-pham',
    'truong-hoc', 'cong-viec', 'cam-xuc', 'suc-khoe', 'du-lich', 'thoi-gian',
    'dia-danh',
}

TOPICS = [
    # ---- 1. Địa danh (đã có) — quốc gia, châu lục, tỉnh thành, vùng miền ----
    ('dia-danh', 'Địa danh', 'Tên quốc gia, châu lục, tỉnh thành và vùng miền', 'map', 'COMPLEX_SIGN', 12, [
        'quoc gia', 'thanh pho', 'thu do', 'phuong dong', 'phuong tay',
        'dong bang song', 'cao nguyen', 'vung mien', 'toan cau', 'thanh thi',
        'do thi', 'nong thon', 'trung uong', 'xich dao', 'dia hinh', 'dia ly',
        'dia diem', 'dia chi', 'quan the', 'dan toc', 'dan toc thieu so',
        'e de', 'au co',
        'ha noi', 'sai gon', 'hue', 'da nang', 'can tho', 'hai phong',
        'nha trang', 'da lat', 'vung tau', 'quy nhon', 'tuy hoa', 'phan thiet',
        'bien hoa', 'ha long', 'sapa', 'sa pa', 'con dao', 'phu quoc', 'hoi an',
        'my tho', 'bac ninh', 'thai binh', 'nam dinh', 'thanh hoa', 'nghe an',
        'xu nghe', 'ha tinh', 'quang binh', 'quang tri', 'quang nam', 'quang ngai',
        'binh dinh', 'phu yen', 'khanh hoa', 'ninh thuan', 'binh thuan', 'kon tum',
        'gia lai', 'dak lak', 'dak nong', 'lam dong', 'binh phuoc', 'tay ninh',
        'binh duong', 'dong nai', 'ba ria', 'long an', 'tien giang', 'ben tre',
        'tra vinh', 'vinh long', 'dong thap', 'an giang', 'kien giang', 'hau giang',
        'soc trang', 'bac lieu', 'ca mau', 'lao cai', 'yen bai', 'dien bien',
        'lai chau', 'son la', 'hoa binh', 'lang son', 'cao bang', 'bac kan',
        'thai nguyen', 'tuyen quang', 'ha giang', 'phu tho', 'vinh phuc',
        'bac giang', 'hai duong', 'hung yen', 'ha nam', 'ninh binh', 'quang ninh',
        'tay bac', 'tay nguyen', 'dong nam', 'dong nam a', 'dong duong',
        'song hong', 'dong bang duyen hai mien trung',
        'lang bac ho', 'lang gom bat trang', 'den hung', 'dao hoang sa',
        'dao ly son', 'dao truong sa', 'deo hai van', 'dia dao cu chi',
        'thanh dia my son', 'thap rua', 'ho hoan kiem', 'cau the huc',
        'vinh ha long', 'quoc tu giam', 'van mieu', 'dinh doc lap',
        'thang long', 'ba na',
        'trung quoc', 'nhat ban', 'han quoc', 'trieu tien', 'thai lan', 'ai lao',
        'campuchia', 'mien dien', 'malaysia', 'indonesia', 'singapore', 'philippin',
        'an do', 'nuoc nga', 'nuoc anh', 'nuoc phap', 'nuoc duc', 'nuoc y',
        'tay ban nha', 'bo dao nha', 'ha lan', 'thuy dien', 'thuy si', 'nuoc uc',
        'canada', 'brazil', 'bra xin', 'mexico', 'a rap', 'do thai', 'tho nhi ky',
        'ai cap', 'nam phi', 'ma cao', 'hong kong', 'dai loan', 'dubai', 'du bai',
        'mong co', 'viet nam', 'chau a', 'chau au', 'chau phi', 'chau my', 'chau uc',
        'nguoi nuoc ngoai', 'albania', 'nuoc lao', 'ma lai', 'nuoc nhat', 'bien dong', 'buon lang',
        'danh lam thang canh', 'bru nay', 'bun ga ri', 'bang la det',
        'chi le', 'cu ba', 'co lom bi a', 'cong hoa sec', 'hung ga ri', 'i rac',
        'italia', 'oa sinh ton', 'pa ki xtan', 'paris', 'pe ru', 'e ti o pi a',
        'dong timor',
    ]),

    # ---- 2. Văn hoá & Lịch sử (mới) ----
    ('van-hoa-lich-su', 'Văn hoá & Lịch sử', 'Nhân vật lịch sử, truyền thuyết và di sản văn hoá', 'book', 'COMPLEX_SIGN', 19, [
        'hung vuong', 'vua hung', 'ho chi minh', 'kim dong', 'long vuong',
        'thanh giong', 'tran quoc toan', 'vo nguyen giap', 'vo thi sau',
        'lac long quan', 'dang van ngu', 'hoang tu', 'cong chua', 'thai tu',
        'trieu dinh', 'vua chua', 'thoi phong kien', 'thuoc dia', 'to tien',
        'truyen thong', 'phong tuc', 'di san', 'di tich', 'tuong dai', 'bao tang',
        'vien bao tang', 'huan chuong', 'huy hieu', 'danh nhan', 'y ec xanh', 'unicef', 'danh hieu', 'chien khu',
    ]),

    # ---- 3. Lễ hội & Ngày kỷ niệm (mới) ----
    ('le-hoi-ngay-ky-niem', 'Lễ hội & Ngày kỷ niệm', 'Các ngày lễ, tết và ngày kỷ niệm trong năm', 'sparkles', 'COMPLEX_SIGN', 20, [
        'ngay quoc te', 'ngay le', 'ngay ky niem', 'ngay thanh lap', 'ngay giai phong',
        'ngay hoi', 'ngay cua', 'ngay khai truong', 'tet han thuc', 'tet nguyen dan',
        'tet duong', 'tet am', 'tet thieu nhi', 'le hoi', 'le hoi hoa trang',
        'giang sinh', 'noel', 'le no en', 'halloween', 'trung thu', 'ram thang tam',
        'mam ngu qua', 'gio to hung vuong', 'le tinh nhan', 'valentine',
        'quoc khanh', 'quoc ca', 'ky niem', 'dam cuoi', 'dam gio', 'co dau',
        'chu re', 'thiep sinh nhat', 'thiep moi', 'lien hoan', 'mit tinh',
        'phao hoa', 'le be giang', 'le vat', 'doan thanh nien', 'doi thieu nien',
        'nhi dong', 'tuan le nguoi diec', 'giao thua',
    ]),

    # ---- 4. Tôn giáo & Tín ngưỡng (mới) ----
    ('ton-giao-tin-nguong', 'Tôn giáo & Tín ngưỡng', 'Các khái niệm tôn giáo, tín ngưỡng và tâm linh', 'star', 'COMPLEX_SIGN', 21, [
        'duc phat', 'nha chua', 'giao hoi', 'ton giao', 'ba than', 'thien chua',
        'kinh thanh', 'nha tho', 'linh muc', 'su thay', 'ni co', 'cau nguyen',
        'tin nguong', 'gia to', 'hoi giao', 'thien duong', 'dia nguc', 'linh hon',
        'con ma', 'con quy', 'ma ca rong', 'boi toan', 'tam linh', 'tho cung',
        'ban tho', 'chua chien', 'thay cung', 'dam ma', 'bat huong',
        'chua huong', 'chua mot cot', 'chua yen tu',
    ]),

    # ---- 5. Pháp luật & Hành chính (mới) ----
    ('phap-luat-hanh-chinh', 'Pháp luật & Hành chính', 'Luật pháp, giấy tờ và thủ tục hành chính', 'grid', 'COMPLEX_SIGN', 22, [
        'phap luat', 'dieu luat', 'toa an', 'canh sat', 'cong an', 'nha tu', 'tu nhan',
        'pham nhan', 'toi pham', 'hop dong', 'chung minh nhan dan', 'can cuoc',
        'ho khau', 'ho chieu', 'giay phep', 'chu ky', 'ky ten', 'quoc hoi', 'chinh phu',
        'nha nuoc', 'chu tich nuoc', 'thu tuong', 'bo truong', 'dai bieu quoc hoi',
        'bau cu', 'khieu nai', 'to cao', 'xu phat', 'phat tien', 'nop thue', 'hai quan',
        'uy ban nhan dan', 'uy ban', 'chinh quyen', 'cong van', 'cong uoc',
        'hinh phat', 'tong thong', 'quan doi', 'dai su', 'dai ta', 'dai tuong',
        'dai uy', 'si quan', 'noi quy', 'doanh trai', 'chu tich',
    ]),

    # ---- 6. Công nghệ thông tin (mới) ----
    ('cong-nghe-thong-tin', 'Công nghệ thông tin', 'Máy tính, điện thoại và internet', 'grid', 'COMPLEX_SIGN', 23, [
        'may vi tinh', 'may tinh', 'dien thoai', 'internet', 'wifi', 'gui email',
        'phan mem', 'ung dung', 'trang web', 'website', 'mang xa hoi', 'facebook',
        'zalo', 'youtube', 'goi video', 'ban phim', 'con chuot may tinh', 'man hinh',
        'may in', 'tin nhan', 'nhan tin', 'chup anh dien thoai', 'may anh', 'may quay',
        'sac pin', 'the nho', 'lap trinh', 'du lieu', 'tai khoan', 'mat khau',
        'dang nhap', 'dang xuat', 'tai xuong', 'tai len', 'ket noi mang', 'may tinh bang',
        'laptop', 'nguoi may', 'con tro', 'ma vach', 'chuot quang',
    ]),

    # ---- 7. Khoa học tự nhiên (mới) ----
    ('khoa-hoc-tu-nhien', 'Khoa học tự nhiên', 'Vật lý, hoá học và các khái niệm khoa học cơ bản', 'sparkles', 'COMPLEX_SIGN', 24, [
        'khoa hoc', 'vat ly', 'hoa hoc', 'sinh hoc', 'chat deo', 'chat huu co',
        'chat long', 'chat ran', 'chat thai', 'chat dot', 'chat doc',
        'chat doc mau da cam', 'chuyen dong', 'chuyen dong quay', 'chuyen dong deu',
        'ma sat', 'quang hop', 'nguyen tu', 'phan tu', 'nang luong', 'phan xa',
        'khoang san', 'kinh hien vi', 'kinh lup', 'thi nghiem', 'phong thi nghiem',
        'dien tu', 'nang luong dien', 'tu truong',
    ]),

    # ---- 8. Toán học & Số liệu (mới) ----
    ('toan-hoc-so-lieu', 'Toán học & Số liệu', 'Các khái niệm số học, hình học và đơn vị đo lường', 'grid', 'SIMPLE_SIGN', 25, [
        'mon toan hoc', 'phep cong', 'phep tru', 'phep nhan', 'phep chia',
        'bang cong', 'bang tru', 'bang nhan', 'bang chia', 'bang cuu chuong',
        'bang nhau', 'lon hon', 'nho hon', 'phan tram', 'phan so', 'so le',
        'so chan', 'so nguyen', 'so thap phan', 'so la ma', 'so lien sau',
        'so lien truoc', 'so do', 'mau so', 'tu so', 'hon so',
        'hinh tron', 'hinh vuong', 'hinh tam giac', 'hinh chu nhat',
        'hinh binh hanh', 'hinh chieu', 'hinh cau', 'hinh elip', 'hinh hoc',
        'hinh lap phuong', 'hinh thang', 'hinh tru', 'hinh tu giac', 'luc giac',
        'goc bet', 'goc nhon', 'goc tu', 'goc vuong', 'duong cheo', 'duong kinh',
        'duong tron', 'duong thang', 'duong cao', 'doan thang', 'diem o giua doan thang',
        'truc hoanh', 'truc tung', 'dai so', 'tap hop', 'tap hop rong',
        'ti le', 'ti so', 'tong so', 'tich so', 'thuong so', 'gia tri tuyet doi',
        'hang dang thuc', 'chu vi', 'dien tich', 'the tich', 'bao nhieu',
        'may gio', 'tinh toan', 'ket qua tinh', 'dap so',
        'ki lo met', 'ki lo gam', 'mi li met', 'hec ta', 'hec to gam', 'hec to met',
        'de ca met', 'de xi met', 'xang ti met', 'binh phuong', 'canh ben',
        'canh day', 'dung tich', 'day so', 'day so lieu',
    ]),

    # ---- 9. Thể thao (mới) ----
    ('the-thao', 'Thể thao', 'Các môn thể thao và hoạt động vận động', 'flame', 'SIMPLE_SIGN', 26, [
        'the thao', 'bong da', 'bong chuyen', 'bong ro', 'bong ban', 'cau long',
        'quan vot', 'tennis', 'boi loi', 'chay bo', 'the duc', 'vo thuat', 'boxing',
        'mon vo', 'judo', 'karate', 'yoga', 'tap gym', 'tap the hinh', 'dua xe',
        'dua thuyen', 'leo nui', 'nhay xa', 'nhay cao', 'nem lao', 'olympic',
        'the van hoi', 'huan luyen vien', 'van dong vien', 'tran dau', 'thi dau',
        'thang cuoc', 'thua cuoc', 'ty so tran dau', 'san van dong', 'ho boi',
        'cau thu', 'trong tai', 'da bong', 'choi bong', 'the duc the thao', 'dau vat',
    ]),

    # ---- 10. Giao thông & Phương tiện (mới) ----
    ('giao-thong-phuong-tien', 'Giao thông & Phương tiện', 'Các phương tiện và hoạt động giao thông', 'map', 'SIMPLE_SIGN', 27, [
        'giao thong', 'phuong tien', 'xe may', 'xe dap', 'xe hoi', 'xe o to',
        'xe buyt', 'xe khach', 'xe tai', 'xe cuu thuong', 'xe cuu hoa', 'xe cap cuu',
        'xe container', 'xe cau', 'xe lam', 'xe lu', 'tau hoa', 'tau thuy',
        'tau dien', 'tau ngam', 'chiec thuyen', 'ca no', 'may bay', 'san bay',
        'ben xe', 'ga tau', 'ben pha', 'boong tau', 'khoang tau', 'bien so xe',
        'bang lai xe', 'den giao thong', 'via he', 'nga tu duong', 'cau vuot',
        'duong ham', 'cau duong', 'lai xe', 'phanh xe', 'tram xang', 'xang',
        'dong co', 'ket xe', 'tac duong', 'tai nan giao thong', 'mu bao hiem',
        'thang may', 'thang cuon', 'xich lo', 'di taxi', 'xe om', 've xe',
        'banh xe', 'toa xe', 'duong sat', 'duong thuy', 'duong hang khong',
        'duong bien gioi', 'duong ray xe lua',
    ]),

    # ---- 11. Mua sắm & Tiền tệ (mới) ----
    ('mua-sam-tien-te', 'Mua sắm & Tiền tệ', 'Tiền bạc, mua bán và cửa hàng', 'grid', 'SITUATION', 28, [
        'tien bac', 'gia tien', 'di mua', 'ban hang', 'cho bua', 'cho noi',
        'cho ben thanh', 'cho noi cai be', 'cho noi cai rang', 'sieu thi',
        'cua hang', 'cua hieu', 'trung tam thuong mai', 'tra gia', 'giam gia',
        'khuyen mai', 'hoa don', 'thanh toan', 'tra tien', 'tien mat', 'the ngan hang',
        'may atm', 'ngan hang', 'vay tien', 'mac no', 'lai suat', 'tiet kiem tien',
        'nguoi giau', 'nguoi ngheo', 'gia dat', 'gia re', 'tien luong', 'thu nhap',
        'chi tieu', 'dong tien', 'do la', 'ngoai te', 'doi tien', 'vi tien', 'tien xu',
        'khach hang', 'giao dich', 'vang bac', 'vang kim loai',
    ]),

    # ---- 12. Nghệ thuật & Giải trí (mới) ----
    ('nghe-thuat-giai-tri', 'Nghệ thuật & Giải trí', 'Âm nhạc, phim ảnh và các hoạt động giải trí', 'sparkles', 'SIMPLE_SIGN', 29, [
        'am nhac', 'bai hat', 'ca hat', 'ca si', 'nhac si', 'nhac cu', 'cay dan',
        'dan guitar', 'dan ghi ta', 'dan piano', 'pi a no', 'duong cam', 'cai trong',
        'cay sao', 'dan violin', 'vi cam', 'dan bau', 'dan to rung', 'bo phim',
        'phim truyen', 'phim tai lieu', 'rap chieu phim', 'dien anh', 'dien vien',
        'dao dien', 've tranh', 'hoi hoa', 'hoa si', 'buc tranh', 'nhiep anh',
        'dieu mua', 'khieu vu', 'nhay mua', 'vu cong', 'san khau', 'vo kich',
        'ganh xiec', 'ao thuat', 'tro choi dien tu', 'tro choi', 'choi game',
        'mon do choi', 'giai tri', 'ca nhac', 'buoi hoa nhac', 'bieu dien', 'may tivi',
        'truyen hinh', 'chuong trinh tivi', 'phim hoat hinh', 'hoat hinh', 'truyen tranh',
        'quyen tieu thuyet', 'bai tho', 'nha van', 'nha tho', 'cong vien giai tri',
        'cong vien', 'vuon thu', 'san choi', 'kich', 'kich ban', 'kich cam',
        'vo cheo', 'doan cai luong', 'nghe si', 'toa soan',
    ]),

    # ---- 13. Quần áo & Phụ kiện (mới) ----
    ('quan-ao-phu-kien', 'Quần áo & Phụ kiện', 'Trang phục và các đồ dùng cá nhân', 'grid', 'SIMPLE_SIGN', 30, [
        'quan ao', 'trang phuc', 'chiec ao', 'chiec quan', 'chiec vay', 'ao so mi',
        'so mi', 'ao khoac', 'ao len', 'ao thun', 'ao dai', 'ao canh', 'ao ke soc',
        'ao phao', 'ao phong', 'ao am', 'quan jean', 'quan bo', 'quan short',
        'quan yem', 'quan au', 'quan dui', 'do lot', 'ao lot', 'doi giay',
        'doi dep', 'giay the thao', 'giay cao got', 'giay ba ta', 'giay dep',
        'doi tat', 'bit tat', 'cai mu', 'non la', 'khan quang co', 'that lung',
        'day nit', 'cai cavat', 'gang tay', 'doi gang tay', 'deo kinh', 'kinh mat',
        'kinh lao', 'dong ho deo tay', 'chiec nhan', 'day chuyen', 'vong tay',
        'vong deo tay', 'hoa tai', 'bong tai', 'do trang suc', 'kim cuong',
        'tui xach', 'cai balo', 'cai vi', 'khuy ao', 'nut ao', 'khoa keo',
        'ao mua', 'cay du', 'dong phuc', 'ao dong phuc', 'yem',
    ]),

    # ---- 14. Màu sắc (mới) ----
    ('mau-sac', 'Màu sắc', 'Tên các màu sắc cơ bản', 'sparkles', 'SIMPLE_SIGN', 31, [
        'mau sac', 'mau do', 'mau xanh', 'mau vang', 'mau tim', 'mau hong', 'mau den',
        'mau trang', 'mau nau', 'mau xam', 'mau cam', 'mau bac', 'mau vang kim',
        'mau xanh la', 'mau xanh duong', 'mau xanh nuoc bien', 'nhieu mau',
        'bang mau', 'bot mau',
    ]),

    # ---- 15. Động vật (mới) ----
    ('dong-vat', 'Động vật', 'Các loài vật nuôi và động vật hoang dã', 'hand', 'SIMPLE_SIGN', 32, [
        'con vat', 'dong vat', 'chim', 'con chim', 'loai vat', 'gia suc', 'gia cam',
        'thu cung', 'vat nuoi', 'chuong trai', 'bay chuot',
        'con cho', 'cho soi', 'cho xu', 'con meo', 'con ga', 'ga mai', 'ga trong',
        'ga rung', 'ga tay', 'con vit', 'con lon', 'con heo', 'con bo', 'bo sat',
        'bo tot', 'con trau', 'con de', 'con cuu', 'con ngua', 'con tho', 'con be',
        'con chuot', 'con voi', 'con ho', 'con su tu', 'con gau', 'con khi',
        'con huou', 'con nai', 'con soi', 'con cao', 'con nhim', 'con soc',
        'con doi', 'con ran', 'con than lan', 'con ca sau', 'con rua', 'con ech',
        'con nhai', 'con ca', 'con tom', 'con cua', 'con oc', 'con muc', 'con so',
        'con hau', 'ca map', 'ca voi', 'ca kiem', 'con quat', 'con cong',
        'con dai bang', 'con cu', 'chim chao mao', 'chim bo cau', 'chim go kien',
        'chim phuong hoang', 'chim sau', 'chim son ca', 'chim se', 'chim yen',
        'con seo', 'con vet', 'con thien nga', 'con hac', 'con ong', 'con buom',
        'con kien', 'dan buom', 'dan kien', 'con ruoi', 'con muoi', 'con gian',
        'con nhen', 'con ve', 'con sau', 'con bo can', 'con chau chau', 'con de men',
        'con ca voi', 'con hai cau', 'con hai ma', 'san ho', 'khung long',
        'con ba ba', 'con chuon chuon', 'con chon', 'con co', 'con coc', 'con cop',
        'con ghe', 'con hoang', 'con luon', 'con ngong', 'con qua', 'con tran',
        'con vuon', 'con dom dom', 'cua dong', 'bay chim',
    ]),

    # ---- 16. Thiên nhiên & Thời tiết (mới) ----
    ('thien-nhien-thoi-tiet', 'Thiên nhiên & Thời tiết', 'Hiện tượng thiên nhiên, cây cối và thời tiết', 'star', 'SIMPLE_SIGN', 33, [
        'thien nhien', 'thoi tiet', 'troi mua', 'con mua', 'mua phun', 'troi nang',
        'con gio', 'con bao', 'giong bao', 'tia set', 'sam chop', 'sam set',
        'tia chop', 'tia nang', 'cau vong', 'suong mu', 'dam may', 'bau troi',
        'mat troi', 'mat trang', 'vang trang', 'ngoi sao', 'vu tru', 'trai dat',
        'qua dat', 'qua dia cau', 'dia cau', 'ngon nui', 'nui', 'nui cao', 'nui thap',
        'dong song', 'con song', 'cai ho', 'bien ca', 'dai duong', 'vinh',
        'con suoi', 'thac nuoc', 'khu rung', 'rung', 'rung nui', 'rung ram',
        'rung thua', 'sa mac', 'hon dao', 'hang dong', 'thung lung', 'cay xanh',
        'cay coi', 'la cay', 'canh cay', 'goc cay', 'ngon cay', 'than cay',
        're cay', 're chum', 're coc', 'mam cay', 'nhuy hoa', 'cuong hoa',
        'cuong la', 'bong hoa', 'hoa cuc', 'hoa dam but', 'hoa giay', 'hoa hong',
        'hoa lay on', 'hoa mai', 'hoa sen', 'hoa dao', 'cay co', 'bui cay',
        'bui tre', 'luy tre', 'cay bang', 'cay go', 'cay khe', 'cay ngo',
        'cay phuong', 'cay sa', 'cay thong', 'cay tre', 'cay trong', 'cay an qua',
        'cay da', 'cay nen', 'xuong rong', 'ruong bac thang', 'dong lua',
        'dong ruong', 'troi nong', 'troi lanh', 'am ap', 'mat me', 'kho han',
        'lu lut', 'lu quet', 'dong dat', 'nui lua', 'song than', 'thien tai',
        'loc xoay', 'mua xuan', 'mua he', 'mua thu', 'mua dong', 'khi hau',
        'moi truong', 'o nhiem moi truong', 'rac thai', 'tai nguyen thien nhien',
        'khong gian', 'khong khi', 'nhiet do', 'nhiet ke', 'hoi nuoc', 'bui phan',
        'lan khoi', 'bui doi', 'ban dao', 'bai cat', 'bai co', 'bai tam',
        'bung binh',
    ]),

    # ---- 17. Cơ thể người (mới) ----
    ('co-the-nguoi', 'Cơ thể người', 'Các bộ phận trên cơ thể người', 'hand', 'SIMPLE_SIGN', 34, [
        'co the nguoi', 'cai dau', 'mai toc', 'toc dai', 'toc ngan', 'toc xoan',
        'doi mat', 'cai mui', 'cai mieng', 'doi moi', 'ham rang', 'rang cua',
        'rang ho', 'rang khenh', 'cai luoi', 'cai cam', 'lo tai', 'cai co',
        'bo vai', 'canh tay', 'khuyu tay', 'co tay', 'ban tay', 'ngon tay',
        'mong tay', 'long nguc', 'cai bung', 'cai lung', 'vong eo', 'cai mong',
        'cai chan', 'dui chan', 'dau goi', 'ban chan', 'ngon chan', 'got chan',
        'bo xuong', 'co bap', 'lan da', 'mau me', 'trai tim', 'la phoi', 'la gan',
        'qua than', 'da day', 'duong ruot', 'bo nao', 'he than kinh', 'than kinh',
        'khop xuong', 'long may', 'mi mat', 'be ma', 'cai trang', 'thai duong',
        'tuy song', 'mach mau', 'thi giac', 'thi luc', 'tinh trung', 'buong trung',
    ]),

    # ---- 18. Nhà cửa & Đồ vật (mới) ----
    ('nha-cua-do-vat', 'Nhà cửa & Đồ vật', 'Nhà ở, nội thất và các vật dụng hằng ngày', 'grid', 'SIMPLE_SIGN', 35, [
        'nha o', 'ngoi nha', 'can ho', 'khu chung cu', 'phong khach', 'phong ngu',
        'phong bep', 'phong tam', 'nha ve sinh', 'bo ban ghe', 'cai ghe', 'cai ban',
        'cai giuong', 'chiec giuong', 'tu quan ao', 'tu lanh', 'may giat',
        'may say quan ao', 'may lanh', 'may dieu hoa', 'dieu hoa', 'cai quat',
        'quat cay', 'quat tran', 'quat treo tuong', 'bong den', 'den long',
        'den pin', 'den vang', 'den xanh', 'den do', 'den dien', 'den ong sao',
        'cai guong', 'rem cua', 'tam tham', 'cai chan', 'cai goi', 'chiec goi',
        'nem giuong', 'dem giuong', 'cai chao', 'cai noi', 'noi com dien',
        'cai xoong', 'cai bat', 'cai dia', 'cai thia', 'cai muong', 'doi dua',
        'con dao', 'cai ly', 'cai coc', 'cai am', 'cai binh', 'phich nuoc',
        'lo vi song', 'bep gas', 'bep dien', 'chia khoa', 'o khoa', 'canh cua',
        'khe cua', 'cua so', 'buc tuong', 'san nha', 'tran nha', 'mai nha',
        'san vuon', 'nha kho', 'thang bo', 'khan tam', 'khan mat', 'khan an',
        'ban chai danh rang', 'kem danh rang', 'banh xa phong', 'xa phong',
        'xa phong giat', 'bot giat', 'dau goi dau', 'giay ve sinh', 'thung rac',
        'cay choi', 'gie lau', 'ban la', 'chuong', 'day dien', 'day thung',
        'day xich', 'dia dvd', 'ong nhom', 'ong nuoc', 'phong bi',
        'oc vit', 'cot nha', 'cot dien', 'cot den', 'chum', 'gia sach', 'to sach',
        'bo sach', 'bo truyen', 'ban ghe', 'chuoi hat', 'bat lua', 'bon rua bat',
        'bang chi dan', 'may bom', 'may chieu', 'may khau', 'may xuc',
        'bam lai xe', 'cong tac', 'ban ui', 'gia treo', 'moc ao', 'mac ao',
        'chuc mung nam moi', 'cai vong', 'cai cua', 'khoa quan', 'bau bi',
        'binh sua', 'khoan', 'cua khau', 'cua kinh', 'nha rong', 'nha tang',
        'lo dot', 'bep lua', 'bien hieu', 'cong truong', 'nha kho', 'thung',
    ]),

    # ---- 19. Cộng đồng Người Điếc & Khuyết tật (mới) ----
    ('cong-dong-nguoi-diec', 'Cộng đồng Người Điếc & Khuyết tật', 'Người khiếm thính, khuyết tật và ngôn ngữ ký hiệu', 'hand', 'SITUATION', 36, [
        'nguoi diec', 'nguoi khiem thinh', 'bi diec', 'ngon ngu ky hieu', 'lam ky hieu',
        'phien dich vien', 'thong dich', 'nguoi khuyet tat', 'bi khuyet tat',
        'khiem thi', 'nguoi mu', 'khiem khuyet', 'may tro thinh', 'oc tai dien tu',
        'cong dong nguoi diec', 'giao duc dac biet', 'hoa nhap cong dong', 'xe lan',
        'chu cai ngon tay', 'ki hieu',
    ]),

    # ---- 20. Sức khoẻ (đã có) ----
    ('suc-khoe', 'Sức khoẻ', 'Bệnh tật, khám chữa bệnh và chăm sóc sức khoẻ', 'flame', 'COMPLEX_SIGN', 9, [
        'suc khoe', 'bi benh', 'bi om', 'con dau', 'bi sot', 'con ho', 'cam cum',
        'benh vien', 'phong kham', 'bac si', 'y ta', 'dieu duong', 'uong thuoc',
        'chich thuoc', 'phau thuat', 'ca mo', 'kham benh', 'chan doan benh',
        'xet nghiem', 'chup x quang', 'sieu am', 'benh ung thu', 'ung thu',
        'tieu duong', 'huyet ap', 'benh tim mach', 'benh nhan', 'cap cuu',
        'vet thuong', 'bang bo', 'khan cap', 'the trang suc khoe', 'khoe manh',
        'yeu ot', 'met moi', 'chong mat', 'buon non', 'bi non', 'tieu chay',
        'di ung', 'nhiem trung', 'vi khuan', 'vac xin', 'tiem chung', 'khau trang',
        'sat khuan', 'dich benh', 'cach ly', 'covid', 'benh tram cam', 'lo au',
        'mang thai', 'sinh con', 'thai nhi', 'nha khoa', 'nho rang', 'chay mau',
        'khat nuoc', 'khang sinh', 'ma tuy', 'ong nghe', 'benh phong', 'benh cui',
        'benh thuy dau', 'benh tat', 'hoi chung dao', 'dau bung', 'dau chan',
        'dau mat', 'dau mat do', 'dau tai', 'dau tay', 'lao phoi', 'sau rang',
        'liet tay chan', 'y te', 'thuoc bac', 'thuoc bo', 'thuoc la', 'tram y te',
        'phong y te',
    ]),

    # ---- 21. Công việc (đã có) ----
    ('cong-viec', 'Công việc', 'Nghề nghiệp và hoạt động làm việc', 'grid', 'SIMPLE_SIGN', 7, [
        'cong viec', 'nghe nghiep', 'di lam', 'viec lam', 'nhan vien', 'giam doc',
        'ong chu', 'ke toan', 'thu ky', 'tiep tan', 'ky su', 'giao vien day hoc',
        'luat su', 'linh cuu hoa', 'dau bep nau an', 'tho may', 'tho hop',
        'nong dan', 'nong nghiep', 'ngu dan', 'dan chai', 'cong nhan', 'tho xay',
        'tho ren', 'tho kim hoan', 'tho theu', 'nghe lai xe', 'phi cong',
        'tiep vien hang khong', 'huong dan vien', 'nha bao', 'phong vien',
        'kien truc su', 'nha khoa hoc', 'nha bac hoc', 'nha toan hoc', 'sinh vien',
        'thuc tap sinh', 'phong van xin viec', 'tuyen dung', 'sa thai', 'nghi viec',
        'hop dong lao dong', 'tang ca', 'nghi phep', 'cong ty', 'doanh nghiep',
        'van phong lam viec', 'dong nghiep', 'cap tren', 'cap duoi', 'quan ly nhan su',
        'duoc thuong', 'chuc vu', 'thang chuc', 'nhan vien ban hang', 'tho dien',
        'tho sua chua', 'thu quy', 'bao ve', 'giup viec nha', 'thu y', 'buu ta',
        'boi ban', 'thu linh', 'sang kien', 'chuyen gia', 'chu nhiem',
    ]),

    # ---- 22. Trường học (đã có) ----
    ('truong-hoc', 'Trường học', 'Học tập, lớp học và giáo dục', 'book', 'SIMPLE_SIGN', 6, [
        'truong hoc', 'lop hoc', 'hoc sinh', 'thay giao', 'co giao', 'thay co',
        'hieu truong', 'ban giam hieu', 'quyen sach', 'sach', 'sach giao khoa',
        'sach toan', 'quyen vo', 'cay but', 'but chi', 'but muc', 'but bi', 'but da',
        'but mau', 'but may', 'thuoc ke', 'cap sach', 'bang den', 'vien phan',
        'phan viet bang', 'ban hoc', 'ghe hoc', 'bai tap ve nha', 'bai kiem tra',
        'ki thi', 'diem so', 'diem thi', 'tot nghiep', 'bang cap', 'truong dai hoc',
        'truong cao dang', 'truong trung hoc', 'truong tieu hoc', 'tieu hoc',
        'truong mau giao', 'truong mam non', 'truong ptcs', 'truong thpt',
        'mon toan', 'mon van', 'mon anh van', 'mon tieng anh', 'mon tieng viet',
        'mon khoa hoc', 'mon lich su', 'mon dia ly', 'mon giao duc cong dan',
        'mon giao duc the chat', 'mon ki thuat', 'mon mi thuat', 'mi thuat',
        'mon ngoai ngu', 'mon sinh hoc', 'mon tin hoc va cong nghe', 'tin hoc',
        'mon tu nhien va xa hoi', 'mon dao duc', 'hoc bai', 'lam bai tap',
        'chep bai', 'giang bai', 'giao bai tap', 'thoi khoa bieu', 'nam hoc',
        'hoc ky', 'ky nghi he', 'di du hoc', 'hoc bong', 'hoc phi', 'thu vien sach',
        'phong thu vien', 'phong mi thuat', 'ban giam khao', 'giao vien chu nhiem',
        'sinh hoat lop', 'lop truong', 'lop pho', 'to truong', 'to chuc',
        'khan quang do', 'kien thuc', 'kinh nghiem', 'ky nang', 'kha nang',
        'cau chuyen', 'cau ghep', 'cau tho', 'cau van', 'cau don', 'cau do',
        'cau doi', 'chu ngu', 'vi ngu', 'bo ngu', 'chinh ta', 'chu hoa', 'chu viet',
        'danh tu', 'danh tu rieng', 'tinh tu', 'tu dong am', 'tu trai nghia',
        'tu ngu', 'cum tu', 'doan van', 'doan tho', 'tap lam van', 'ngu phap',
        'ngu dieu', 'luyen tu va cau', 'do dung day hoc', 'do dung hoc tap',
        'ngoai hinh', 'ban giam khao', 'gia sach', 'san truong', 'cong truong hoc',
        'hoc gioi', 'hoc kem', 'hoc trung binh', 'hoc nhom', 'thieu nhi', 'thieu nien',
        'thanh nien', 'goc hoc tap', 'danh ba',
    ]),

    # ---- 23. Thực phẩm (đã có) ----
    ('thuc-pham', 'Thực phẩm', 'Món ăn, đồ uống và các loại thực phẩm', 'grid', 'SIMPLE_SIGN', 5, [
        'thuc pham', 'do an', 'mon an', 'com trang', 'to pho', 'bat bun', 'mi goi',
        'mi tom', 'mi van than', 'mi y', 'banh mi', 'chao dinh duong', 'xoi',
        'thit lon', 'thit bo', 'thit ga', 'thit vit', 'con ca', 'con tom',
        'con cua bien', 'trung ga', 'sua', 'sua tuoi', 'sua chua',
        'pho mai', 'bo sua', 'dau nanh', 'dau phu', 'rau', 'rau cu qua',
        'rau cai', 'rau cai thao', 'rau diep ca', 'rau den', 'rau mung toi',
        'rau ngo', 'rau mui', 'rau ngot', 'ca chua', 'khoai tay', 'khoai lang',
        'ca rot', 'bap cai', 'sup lo', 'cu hanh', 'cu toi', 'trai ot', 'qua ot',
        'cu gung', 'hat muoi', 'muoi', 'duong cat', 'nuoc mam', 'tuong ot',
        'dam an', 'dau an', 'hat gao', 'gao nep', 'thoc', 'trai ngo', 'bap ngo',
        'cu khoai', 'to sup', 'bat canh', 'noi lau', 'do nuong', 'do chien',
        'mon xao', 'do luoc', 'mon hap', 'trai cay', 'hoa qua', 'trai chuoi',
        'trai cam', 'trai buoi', 'trai xoai', 'trai oi', 'trai du du', 'trai dua hau',
        'trai dua', 'chum nho', 'trai tao', 'trai le', 'trai dao', 'trai mit',
        'trai sau rieng', 'trai chom chom', 'trai nhan', 'trai vai', 'trai thanh long',
        'trai me', 'trai coc',
        'qua buoi', 'qua bau', 'qua cam', 'qua chanh', 'qua chuoi', 'qua chom chom',
        'qua dua hau', 'qua dua', 'qua dua xiem', 'qua gac', 'qua hong',
        'qua hong xiem', 'qua khe', 'qua le', 'qua mit', 'qua muop', 'qua na',
        'qua nho', 'qua nhan', 'qua quyt', 'qua quat', 'qua roi', 'qua su su',
        'qua sau rieng', 'qua thanh long', 'qua tao', 'qua vai', 'qua xoai',
        'qua dieu', 'qua du du', 'qua oi',
        'nuoc uong', 'nuoc loc', 'nuoc ngot', 'nuoc trai cay', 'ly sinh to', 'sinh to',
        'ca phe sua', 'ca phe', 'tach tra', 'tra', 'tra nong', 'tra sua', 'tra da',
        'lon bia', 'bia', 'chai ruou', 'ruou', 'vien nuoc da', 'vien keo',
        'thoi socola', 'chocolate', 'socola', 'cocacola', '7up', 'que kem', 'kem',
        'banh keo', 'banh sinh nhat', 'banh chung', 'banh tet', 'nau an',
        'dau bep nha hang', 'nha bep', 'goi cuon', 'cha gio', 'cha ca', 'to hu tieu',
        'to mi quang', 'dia com tam', 'buoi an sang', 'buoi an trua', 'buoi an toi',
        'do an vat', 'buoi tiec', 'tiec', 'nha hang an uong', 'quan cafe',
        'thuc don mon an', 'goi mon an', 'banh', 'banh bot loc', 'banh canh',
        'banh cuon', 'banh cot', 'banh gai', 'banh gio', 'banh giay', 'banh hamburger',
        'banh pizza', 'banh pia', 'banh sandwich', 'banh trang', 'banh da',
        'banh duc', 'bun cha', 'bun mam', 'bun ngan', 'bun dau', 'bun oc',
        'chanh muoi', 'chanh nong', 'chanh da', 'chao suon', 'che do do',
        'com binh dan', 'com rang', 'ga nuong lu', 'hat tieu', 'hat lac', 'hat cuom',
        'mien', 'mut', 'muoi i ot', 'mia', 'mam com', 'mo rau',
        'mat ong', 'boc ngo', 'goi nom', 'ca kho', 'bong ngo', 'com',
    ]),

    # ---- 24. Gia đình (đã có) ----
    ('gia-dinh', 'Gia đình', 'Các thành viên trong gia đình và họ hàng', 'user', 'SIMPLE_SIGN', 3, [
        'gia dinh', 'bo va me', 'cha va me', 'nguoi cha', 'nguoi me',
        'ong noi', 'ba noi', 'ba ngoai', 'ong ngoai', 'anh trai', 'chi gai',
        'em trai', 'em gai', 'anh chi em', 'anh chi', 'anh em', 'con trai',
        'con gai', 'con cai', 'dua chau', 'chau trai', 'chau gai', 'chau ngoai',
        'chau noi', 'chau ho', 'co ruot', 'chu ruot', 'bac ruot', 'di ruot',
        'cau ruot', 'me ho', 'thim', 'ho hang', 'nguoi vo', 'nguoi chong',
        'vo chong', 'ket hon', 'ly hon', 'anh re', 'chi dau', 'em re', 'em dau',
        'me chong', 'bo chong', 'me vo', 'bo vo', 'con dau', 'con re', 'con nuoi',
        'me ke', 'bo duong', 'anh em ho', 'sinh doi', 'song than', 'con mot',
        'ho va ten', 'to tien', 'ong ba', 'the he gia dinh', 'anh hai', 'anh ca',
        'chi hai', 'chi cal', 'anh vo', 'anh ho', 'anh ruot', 'chi ho', 'chi chong',
        'em ho', 'em ut', 'be gai', 'be trai', 'ban gai', 'ban trai',
    ]),

    # ---- 25. Cảm xúc (đã có) ----
    ('cam-xuc', 'Cảm xúc', 'Trạng thái cảm xúc và tâm trạng', 'sparkles', 'SIMPLE_SIGN', 8, [
        'cam xuc', 'vui ve', 'buon ba', 'tuc gian', 'so hai', 'lo lang', 'lo au',
        'hanh phuc', 'hoi hop', 'ngac nhien', 'that vong', 'xau ho', 'tu hao',
        'ghen ty', 'ghen tuong', 'yeu thuong', 'ghet bo', 'yeu thich', 'khong thich',
        'chan nan', 'co don', 'nho nhung', 'thuong yeu', 'giu binh tinh',
        'bat man', 'hoi han', 'ay nay', 'ngai ngung', 'tu tin', 'mac cam', 'bi stress',
        'cang thang', 'thu gian', 'thoai mai', 'de chiu', 'kho chiu', 'buc boi',
        'buc minh', 'phan khich', 'hao hung', 'tuyet vong', 'hy vong', 'tin tuong',
        'nghi ngo', 'suy nghi', 'lung tung', 'boi roi', 'kinh ngac', 'khiep so',
        'hoang so', 'nu cuoi', 'khoc loc', 'mim cuoi', 'cuoi lon', 'cuoi nhech mep',
        'cuoi vo bung', 'noi da ga', 'ron da', 'run ray', 'nong nay', 'diem tinh',
        'nghi luc', 'y chi', 'y thuc', 'y tuong', 'niem vui',
    ]),

    # ---- 26. Thời gian (đã có) ----
    ('thoi-gian', 'Thời gian', 'Ngày, tháng, năm và các mốc thời gian', 'star', 'SIMPLE_SIGN', 11, [
        'thoi gian', 'may gio roi', 'bao nhieu phut', 'bao nhieu giay', 'moi ngay',
        'moi tuan', 'moi thang', 'moi nam', 'hom nay', 'hom qua', 'ngay mai',
        'hien tai', 'qua khu', 'tuong lai', 'den som', 'den muon', 'buoi sang',
        'buoi trua', 'buoi chieu', 'buoi toi', 'ban dem', 'buoi dem', 'nua dem',
        'thu hai', 'thu ba', 'thu tu', 'thu nam', 'thu sau', 'thu bay', 'chu nhat',
        'cuoi tuan', 'dau tuan', 'tuan nay', 'tuan sau', 'tuan truoc', 'tuan trang mat',
        'thang gieng', 'thang chap', 'quy nam', 'thap ky', 'the ky', 'quyen lich',
        'am lich', 'duong lich', 'dong ho treo tuong', 'bao lau roi', 'khi nao',
        'luc nao', 'thuong xuyen', 'thinh thoang', 'hiem khi', 'mai mai', 'ngay xua',
        've sau', 'truoc day', 'nghi le', 'ngay nay', 'ngay sinh', 'ngay thang',
        'ngay dem', 'nam ngoai', 'nam toi', 'cuoi nam', 'cuoi thang',
    ]),

    # ---- 27. Giao tiếp hằng ngày (đã có) ----
    ('giao-tiep-hang-ngay', 'Giao tiếp hằng ngày', 'Chào hỏi và các câu giao tiếp thường dùng', 'hand', 'SITUATION', 4, [
        'xin chao', 'loi chao', 'tam biet', 'hen gap lai', 'cam on', 'xin loi',
        'khong sao dau', 'lam on', 'vui long', 'loi chuc mung', 'chuc mung',
        'dong y', 'khong dong y', 'duoc roi', 'khong duoc', 'da vang', 'ten la gi',
        'gioi thieu ban than', 'lam quen', 'hoi tham', 'tra loi cau hoi',
        'noi chuyen', 'cau hoi', 'hoi dap', 'goi dien thoai', 'gap go nhau',
        'lich hen', 'hen ho', 'gap mat', 'chao hoi', 'xin phep', 'cho phep',
        'yeu cau', 'loi de nghi', 'loi moi', 'khach moi', 'tu choi', 'chap nhan',
        'thoa thuan', 'loi hua', 'giai thich', 'noi lai', 'hieu y', 'khong hieu',
        'da ro chua', 'khong biet', 'giong noi', 'giong ca', 'tieng noi', 'net mat',
    ]),

    # ---- 28. Du lịch (đã có) ----
    ('du-lich', 'Du lịch', 'Đi lại, khách sạn và các hoạt động du lịch', 'map', 'COMPLEX_SIGN', 10, [
        'du lich', 'di choi xa', 'khach san', 'nha nghi', 'dat phong khach san',
        'lam thu tuc check in', 'hanh ly du lich', 'vali', 'va li',
        'huong dan vien du lich', 'diem den du lich', 'diem tham quan',
        'di tham quan', 've may bay', 'phi truong', 'ban do du lich', 'la ban',
        'khach du lich', 'khach', 'nghi duong', 'di nghi mat', 'kham pha the gioi',
        'di phuot', 'bai bien', 'khu resort', 'chuyen du lich', 'du thuyen',
        'le trai', 'phao', 'hai dao',
    ]),

    # ---- 29. Đại từ & Từ ngữ pháp (mới) ----
    ('dai-tu-lien-tu', 'Đại từ & Từ ngữ pháp', 'Đại từ, liên từ và các từ nối câu', 'grid', 'SIMPLE_SIGN', 37, [
        'chung ta', 'chung toi', 'bon ho', 'cai gi', 'o dau', 'tai sao vay',
        'the nao day', 'vi sao', 'nhung ma', 'neu nhu', 'boi vi', 'cho nen',
        'tuy nhien', 'mac du', 'du sao', 'toan bo', 'tat ca moi nguoi', 'mot vai',
        'mot so', 'con thieu', 'them vao', 'ban than',
        'ben canh', 'ben duoi', 'ben phai', 'ben trong', 'ben trai', 'ben tren',
    ]),

    # ---- 30. Số đếm (đã có) ----
    ('so-dem', 'Số đếm', 'Chữ số và cách đếm', 'grid', 'SIMPLE_SIGN', 2, [
        'so dem', 'con so', 'so mot', 'so hai', 'so ba', 'so bon', 'so nam', 'so sau',
        'so bay', 'so tam', 'so chin', 'so muoi', 'hang tram', 'hang nghin',
        'hang trieu', 'hang ty', 'hang chuc', 'hang don vi', 'hang doc', 'hang ngang',
        'chuc nghin', 'chuc trieu', 'thu nhat', 'thu hai', 'thu ba', 'dau tien',
        'cuoi cung', 'so luong', 'dem so', 'mot cap', 'mot doi', 'mot minh',
    ]),

    # ---- 31. Bảng chữ cái (đã có) ----
    ('bang-chu-cai', 'Bảng chữ cái', 'Chữ cái ngón tay và dấu thanh tiếng Việt', 'book', 'SIMPLE_SIGN', 1, [
        'bang chu cai', 'chu cai', 'nguyen am', 'phu am', 'dau sac', 'dau huyen',
        'dau hoi', 'dau nga', 'dau nang', 'danh van', 'dau bang', 'dau chia',
        'dau cham', 'dau cham hoi', 'dau cham than', 'dau gach ngang', 'dau hai cham',
        'dau ngoac don', 'dau ngoac kep', 'dau nhan', 'dau phay',
    ]),

    # ---- 32. Hành động & Tính chất thông dụng (mới — bắt các động từ/tính từ chung) ----
    ('hanh-dong-tinh-chat-chung', 'Hành động & Tính chất thông dụng', 'Các động từ và tính từ thường gặp trong đời sống', 'sparkles', 'SIMPLE_SIGN', 38, []),

    # ---- 33. Từ vựng khác (mới — hứng phần còn lại) ----
    ('tu-vung-khac', 'Từ vựng khác', 'Các từ vựng chưa xếp vào chủ đề cụ thể', 'grid', 'SIMPLE_SIGN', 39, []),
]
