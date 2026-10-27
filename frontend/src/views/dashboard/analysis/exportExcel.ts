/**
 * Xuất số liệu Dashboard ra Excel (exceljs) — kẻ viền, tô màu header theo nhóm chỉ số.
 *
 * Mọi bảng đi qua cùng một hàm `writeTable` nên trông đồng nhất:
 *   dòng 1  tiêu đề (nền xanh đậm)
 *   dòng 2  mô tả khoảng thời gian / thời điểm xuất
 *   dòng 3  tên NHÓM chỉ số, mỗi nhóm một màu đậm
 *   dòng 4  tên cột, nền nhạt cùng màu với nhóm
 *   …       dữ liệu kẻ viền, hàng xen màu; cuối bảng có dòng tổng (nếu cần)
 */
import ExcelJS from 'exceljs';
import type { DashboardModel, NamedValue, UserStatModel } from '@/api/dashboard';

// ---------------------------------------------------------------- bảng màu theo loại chỉ số

export type Group = 'info' | 'access' | 'learn' | 'course' | 'revenue' | 'user';

const GROUP_COLOR: Record<Group, { dark: string; light: string }> = {
  info: { dark: '2F5597', light: 'DDEBF7' }, // thông tin người dùng
  user: { dark: '1F7A8C', light: 'D9F0F4' }, // người dùng (số lượng)
  access: { dark: 'C55A11', light: 'FCE4D6' }, // truy cập
  learn: { dark: '548235', light: 'E2EFDA' }, // học tập
  course: { dark: '7030A0', light: 'EDE4F5' }, // khoá học
  revenue: { dark: 'BF8F00', light: 'FFF2CC' }, // doanh thu
};

const TITLE_FILL = '1F3864';
const ZEBRA_FILL = 'F7F9FC';
const TOTAL_FILL = 'EDEDED';
const BORDER_COLOR = 'BFBFBF';
const FONT = 'Calibri';

const fill = (argb: string): ExcelJS.Fill => ({ type: 'pattern', pattern: 'solid', fgColor: { argb: `FF${argb}` } });
const thin: Partial<ExcelJS.Border> = { style: 'thin', color: { argb: `FF${BORDER_COLOR}` } };
const BORDER: Partial<ExcelJS.Borders> = { top: thin, left: thin, bottom: thin, right: thin };

// ---------------------------------------------------------------- mô tả bảng

type Fmt = 'text' | 'int' | 'money' | 'percent' | 'date' | 'datetime';

export interface Column<T> {
  header: string;
  width: number;
  value: (row: T) => string | number | Date | null | undefined;
  fmt?: Fmt;
  /** Cộng ở dòng tổng (chỉ cột số) */
  sum?: boolean;
}

export interface ColumnGroup<T> {
  label: string;
  group: Group;
  columns: Column<T>[];
}

const NUM_FMT: Partial<Record<Fmt, string>> = {
  int: '#,##0',
  money: '#,##0" đ"',
  percent: '0.0"%"',
  date: 'dd/mm/yyyy',
  datetime: 'dd/mm/yyyy hh:mm',
};

/**
 * Ghi một bảng có nhóm cột vào sheet, bắt đầu từ dòng `startRow`. Trả dòng kế tiếp còn trống.
 * Dòng tổng dùng công thức SUM để người xem sửa số liệu thì tổng tự cập nhật.
 */
function writeTable<T>(
  ws: ExcelJS.Worksheet,
  opts: {
    title: string;
    subtitle?: string;
    groups: ColumnGroup<T>[];
    rows: T[];
    totals?: boolean;
    startRow?: number;
    /** Cố định tiêu đề + bộ lọc trên dòng tên cột; tắt khi một sheet có nhiều bảng xếp chồng */
    filter?: boolean;
  },
): number {
  const cols = opts.groups.flatMap((g) => g.columns.map((c) => ({ ...c, group: g.group })));
  const n = cols.length;
  let r = opts.startRow ?? 1;

  cols.forEach((c, i) => {
    const col = ws.getColumn(i + 1);
    col.width = Math.max(col.width ?? 0, c.width);
  });
  // In ra giấy: khổ ngang, co vừa chiều rộng một trang, lặp lại dòng tiêu đề bảng
  ws.pageSetup.orientation = 'landscape';
  ws.pageSetup.fitToPage = true;
  ws.pageSetup.fitToWidth = 1;
  ws.pageSetup.fitToHeight = 0;
  ws.pageSetup.margins = { left: 0.4, right: 0.4, top: 0.5, bottom: 0.5, header: 0.3, footer: 0.3 };

  // Tiêu đề
  ws.mergeCells(r, 1, r, n);
  const title = ws.getCell(r, 1);
  title.value = opts.title;
  title.font = { name: FONT, size: 15, bold: true, color: { argb: 'FFFFFFFF' } };
  title.fill = fill(TITLE_FILL);
  title.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
  ws.getRow(r).height = 28;
  r++;

  if (opts.subtitle) {
    ws.mergeCells(r, 1, r, n);
    const sub = ws.getCell(r, 1);
    sub.value = opts.subtitle;
    sub.font = { name: FONT, size: 10, italic: true, color: { argb: 'FF595959' } };
    sub.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
    ws.getRow(r).height = 18;
    r++;
  }

  // Dòng nhóm chỉ số: gộp ô theo nhóm, nền đậm
  let c = 1;
  for (const g of opts.groups) {
    const from = c;
    const to = c + g.columns.length - 1;
    if (to > from) ws.mergeCells(r, from, r, to);
    const cell = ws.getCell(r, from);
    cell.value = g.label.toUpperCase();
    for (let k = from; k <= to; k++) {
      const x = ws.getCell(r, k);
      x.fill = fill(GROUP_COLOR[g.group].dark);
      x.border = BORDER;
    }
    cell.font = { name: FONT, size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
    cell.alignment = { vertical: 'middle', horizontal: 'center' };
    c = to + 1;
  }
  ws.getRow(r).height = 22;
  r++;

  // Tên cột: nền nhạt cùng màu nhóm
  const headerRow = r;
  cols.forEach((col, i) => {
    const cell = ws.getCell(r, i + 1);
    cell.value = col.header;
    cell.font = { name: FONT, size: 11, bold: true, color: { argb: `FF${GROUP_COLOR[col.group].dark}` } };
    cell.fill = fill(GROUP_COLOR[col.group].light);
    cell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
    cell.border = BORDER;
  });
  ws.getRow(r).height = 36;
  r++;

  // Dữ liệu
  const firstData = r;
  opts.rows.forEach((row, idx) => {
    cols.forEach((col, i) => {
      const cell = ws.getCell(r, i + 1);
      const v = col.value(row);
      cell.value = v === undefined || v === null || v === '' ? null : v;
      cell.font = { name: FONT, size: 11 };
      cell.border = BORDER;
      if (col.fmt && NUM_FMT[col.fmt]) cell.numFmt = NUM_FMT[col.fmt]!;
      const isNum = col.fmt && col.fmt !== 'text';
      cell.alignment = { vertical: 'middle', horizontal: isNum ? 'right' : 'left', indent: isNum ? 0 : 1 };
      if (idx % 2 === 1) cell.fill = fill(ZEBRA_FILL);
    });
    ws.getRow(r).height = 20;
    r++;
  });
  const lastData = r - 1;

  // Dòng tổng
  if (opts.totals && opts.rows.length) {
    cols.forEach((col, i) => {
      const cell = ws.getCell(r, i + 1);
      const letter = ws.getColumn(i + 1).letter;
      if (i === 0) cell.value = 'Tổng cộng';
      else if (col.sum) cell.value = { formula: `SUM(${letter}${firstData}:${letter}${lastData})` };
      cell.font = { name: FONT, size: 11, bold: true };
      cell.fill = fill(TOTAL_FILL);
      cell.border = { ...BORDER, top: { style: 'medium', color: { argb: 'FF7F7F7F' } } };
      if (col.fmt && NUM_FMT[col.fmt]) cell.numFmt = NUM_FMT[col.fmt]!;
      cell.alignment = { vertical: 'middle', horizontal: col.sum ? 'right' : 'left', indent: col.sum ? 0 : 1 };
    });
    ws.getRow(r).height = 22;
    r++;
  }

  // Bộ lọc trên dòng tên cột (chỉ khi bảng là bảng chính của sheet)
  if (opts.filter !== false && (opts.startRow ?? 1) === 1 && opts.rows.length) {
    ws.autoFilter = { from: { row: headerRow, column: 1 }, to: { row: lastData, column: n } };
    ws.views = [{ state: 'frozen', ySplit: headerRow, xSplit: 1 }];
  }
  return r + 1;
}

// ---------------------------------------------------------------- tiện ích

function newWorkbook() {
  const wb = new ExcelJS.Workbook();
  wb.creator = 'SignAI CMS';
  wb.created = new Date();
  return wb;
}

async function save(wb: ExcelJS.Workbook, name: string) {
  const buf = await wb.xlsx.writeBuffer();
  const blob = new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${name}_${today()}.xlsx`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

const pad = (x: number) => String(x).padStart(2, '0');
function today() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}
function nowText() {
  const d = new Date();
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
const dayText = (iso: string) => iso.split('-').reverse().join('/');
/**
 * exceljs ghi ngày giờ theo UTC, Excel lại hiển thị nguyên giá trị đó. Nên dựng Date có các thành phần
 * UTC bằng đúng giờ địa phương của người xuất — không thì ngày lùi một hôm và giờ lệch 7 tiếng (giờ VN).
 */
const toDate = (iso?: string | null) => {
  if (!iso) return null;
  const t = new Date(iso);
  return new Date(Date.UTC(t.getFullYear(), t.getMonth(), t.getDate(), t.getHours(), t.getMinutes(), t.getSeconds()));
};
/** Ngày dạng 'YYYY-MM-DD' (không có giờ) → đúng ngày đó trong Excel */
const isoDay = (iso: string) => {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
};
const rangeText = (d: DashboardModel) =>
  `Khoảng ${dayText(d.fromDate)} – ${dayText(d.toDate)} (${d.days} ngày), so với ${d.days} ngày liền trước · Xuất lúc ${nowText()}`;

// ---------------------------------------------------------------- 1. Tổng quan chỉ số

interface KpiRow {
  name: string;
  current: number | null;
  previous: number | null;
  fmt: Fmt;
}

export async function exportOverview(d: DashboardModel) {
  const wb = newWorkbook();
  const ws = wb.addWorksheet('Tổng quan', { properties: { tabColor: { argb: `FF${TITLE_FILL}` } } });
  const kpi = (name: string, k: { current: number; previous: number }, fmt: Fmt = 'int'): KpiRow => ({
    name, current: k.current, previous: k.previous, fmt,
  });
  const one = (name: string, v: number, fmt: Fmt = 'int'): KpiRow => ({ name, current: v, previous: null, fmt });

  const sections: { label: string; group: Group; rows: KpiRow[] }[] = [
    {
      label: 'Truy cập',
      group: 'access',
      rows: [
        kpi('Lượt xem trang', d.pageViews),
        kpi('Khách truy cập', d.visitors),
        kpi('Phiên truy cập', d.sessions),
        kpi('Người dùng hoạt động', d.activeUsers),
        one('Số trang / phiên', d.pagesPerSession, 'text'),
        one('Khách quay lại (%)', d.returningVisitorRate, 'percent'),
        one('Hoạt động hôm nay (DAU)', d.dau),
        one('Hoạt động 7 ngày (WAU)', d.wau),
        one('Hoạt động 30 ngày (MAU)', d.mau),
        one('Độ gắn bó DAU/MAU (%)', d.stickiness, 'percent'),
      ],
    },
    {
      label: 'Doanh thu',
      group: 'revenue',
      rows: [
        kpi('Doanh thu', d.revenue, 'money'),
        kpi('Đơn đã thanh toán', d.paidOrders),
        kpi('Người trả tiền', d.payingUsers),
        one('Tổng doanh thu từ trước tới nay', d.revenueAllTime, 'money'),
        one('Đơn đã tạo', d.ordersCreated),
        one('Đơn đang chờ chuyển khoản', d.ordersPending),
        one('Đơn hết hạn không trả', d.ordersExpired),
        one('Tỉ lệ chuyển đổi (%)', d.conversionRate, 'percent'),
        one('Doanh thu TB / người trả tiền', d.arppu, 'money'),
        one('Giao dịch chưa khớp đơn', d.unmatchedTransactions),
      ],
    },
    {
      label: 'Người dùng',
      group: 'user',
      rows: [
        one('Tổng người dùng', d.totalUsers),
        kpi('Đăng ký mới', d.newUsers),
        one('Đang dùng Premium', d.premiumActive),
        one('Tỉ lệ Premium (%)', d.premiumRate, 'percent'),
      ],
    },
    {
      label: 'Học tập & cộng đồng',
      group: 'learn',
      rows: [
        one('Người có học', d.learning.learners),
        one('Phút học', d.learning.minutesStudied),
        one('Bài học hoàn thành', d.learning.lessonsCompleted),
        one('Lượt ôn từ', d.learning.signsReviewed),
        one('Bài kiểm tra đã làm', d.learning.quizzesTaken),
        one('Lượt chấm điểm AI', d.learning.aiChecksDone),
        one('Bài viết diễn đàn mới', d.forumPosts),
        one('Bình luận mới', d.forumComments),
      ],
    },
  ];

  let row = 1;
  sections.forEach((s, i) => {
    row = writeTable<KpiRow>(ws, {
      title: i === 0 ? 'TỔNG QUAN CHỈ SỐ – SIGNAI' : s.label,
      subtitle: i === 0 ? rangeText(d) : undefined,
      startRow: row,
      filter: false,
      rows: s.rows,
      groups: [
        {
          label: s.label,
          group: s.group,
          columns: [
            { header: 'Chỉ số', width: 36, value: (r) => r.name },
            { header: 'Kỳ này', width: 18, value: (r) => r.current, fmt: 'int' },
            { header: 'Kỳ trước', width: 18, value: (r) => r.previous, fmt: 'int' },
            {
              header: 'Thay đổi',
              width: 14,
              fmt: 'percent',
              value: (r) =>
                r.previous == null || r.current == null || r.previous === 0
                  ? null
                  : Math.round(((r.current - r.previous) / r.previous) * 1000) / 10,
            },
          ],
        },
      ],
    });
    // Định dạng riêng từng dòng (tiền, %, số thập phân) cho cột "Kỳ này" / "Kỳ trước"
    s.rows.forEach((r, k) => {
      const dataRow = row - 1 - s.rows.length + k;
      for (const col of [2, 3]) {
        const cell = ws.getCell(dataRow, col);
        if (r.fmt === 'money') cell.numFmt = NUM_FMT.money!;
        else if (r.fmt === 'percent') cell.numFmt = NUM_FMT.percent!;
        else if (r.fmt === 'text') cell.numFmt = '0.00';
      }
    });
  });
  ws.views = [{ state: 'frozen', ySplit: 2 }];
  await save(wb, 'tong-quan-chi-so');
}

// ---------------------------------------------------------------- 2. Xu hướng theo ngày

export async function exportDaily(d: DashboardModel) {
  const wb = newWorkbook();
  const ws = wb.addWorksheet('Theo ngày', { properties: { tabColor: { argb: `FF${GROUP_COLOR.access.dark}` } } });
  type P = DashboardModel['daily'][number];
  writeTable<P>(ws, {
    title: 'XU HƯỚNG THEO NGÀY – SIGNAI',
    subtitle: rangeText(d),
    rows: d.daily,
    totals: true,
    groups: [
      { label: 'Ngày', group: 'info', columns: [{ header: 'Ngày', width: 14, value: (p) => isoDay(p.date), fmt: 'date' }] },
      {
        label: 'Truy cập',
        group: 'access',
        columns: [
          { header: 'Lượt xem trang', width: 15, value: (p) => p.pageViews, fmt: 'int', sum: true },
          { header: 'Khách truy cập', width: 15, value: (p) => p.visitors, fmt: 'int', sum: true },
          { header: 'Khách đã đăng nhập', width: 17, value: (p) => p.signedInVisitors, fmt: 'int', sum: true },
        ],
      },
      {
        label: 'Người dùng',
        group: 'user',
        columns: [{ header: 'Đăng ký mới', width: 14, value: (p) => p.newUsers, fmt: 'int', sum: true }],
      },
      {
        label: 'Học tập',
        group: 'learn',
        columns: [
          { header: 'Người có học', width: 14, value: (p) => p.learners, fmt: 'int', sum: true },
          { header: 'Phút học', width: 12, value: (p) => p.minutesStudied, fmt: 'int', sum: true },
        ],
      },
      {
        label: 'Doanh thu',
        group: 'revenue',
        columns: [
          { header: 'Doanh thu', width: 16, value: (p) => p.revenue, fmt: 'money', sum: true },
          { header: 'Đơn đã trả', width: 12, value: (p) => p.paidOrders, fmt: 'int', sum: true },
        ],
      },
    ],
  });
  await save(wb, 'xu-huong-theo-ngay');
}

// ---------------------------------------------------------------- 3. Tính năng & nguồn truy cập

export async function exportTraffic(
  d: DashboardModel,
  labels: { route: (n: string | null) => string; device: (n: string | null) => string; kind: (n: string | null) => string },
) {
  const wb = newWorkbook();
  const sheet = (name: string, title: string, items: NamedValue[], nameHeader: string, label: (n: string | null) => string, valueHeader: string, extraHeader?: string) => {
    const ws = wb.addWorksheet(name, { properties: { tabColor: { argb: `FF${GROUP_COLOR.access.dark}` } } });
    const total = items.reduce((s, x) => s + x.value, 0);
    const columns: Column<NamedValue>[] = [
      { header: nameHeader, width: 32, value: (x) => label(x.name) },
      { header: valueHeader, width: 16, value: (x) => x.value, fmt: 'int', sum: true },
      { header: 'Tỉ lệ', width: 12, value: (x) => (total ? Math.round((x.value / total) * 1000) / 10 : 0), fmt: 'percent' },
    ];
    if (extraHeader) columns.splice(2, 0, { header: extraHeader, width: 16, value: (x) => x.extra ?? null, fmt: 'int' });
    writeTable<NamedValue>(ws, {
      title: title.toUpperCase(),
      subtitle: rangeText(d),
      rows: items,
      totals: true,
      groups: [{ label: 'Truy cập', group: 'access', columns }],
    });
  };
  sheet('Tính năng', 'Tính năng được xem nhiều', d.topRoutes, 'Tính năng', labels.route, 'Lượt xem', 'Số khách');
  sheet('Thiết bị', 'Khách truy cập theo thiết bị', d.devices, 'Thiết bị', labels.device, 'Số khách');
  sheet('Nguồn truy cập', 'Phiên truy cập theo nguồn', d.referrers, 'Nguồn', (n) => n ?? 'Vào thẳng / không rõ', 'Số phiên');
  sheet('Khung giờ', 'Lượt xem theo giờ trong ngày (giờ VN)',
    d.hourly.map((v, h) => ({ name: `${pad(h)}:00 – ${pad(h)}:59`, value: v })), 'Khung giờ', (n) => n ?? '', 'Lượt xem');

  // Loại tài khoản thuộc nhóm "người dùng" chứ không phải truy cập
  const ws = wb.addWorksheet('Loại tài khoản', { properties: { tabColor: { argb: `FF${GROUP_COLOR.user.dark}` } } });
  const totalUsers = d.accountKinds.reduce((s, x) => s + x.value, 0);
  writeTable<NamedValue>(ws, {
    title: 'NGƯỜI DÙNG THEO LOẠI TÀI KHOẢN',
    subtitle: `Toàn bộ người dùng · Xuất lúc ${nowText()}`,
    rows: d.accountKinds,
    totals: true,
    groups: [
      {
        label: 'Người dùng',
        group: 'user',
        columns: [
          { header: 'Loại tài khoản', width: 32, value: (x) => labels.kind(x.name) },
          { header: 'Số người dùng', width: 16, value: (x) => x.value, fmt: 'int', sum: true },
          { header: 'Tỉ lệ', width: 12, value: (x) => (totalUsers ? Math.round((x.value / totalUsers) * 1000) / 10 : 0), fmt: 'percent' },
        ],
      },
    ],
  });
  await save(wb, 'tinh-nang-va-nguon-truy-cap');
}

// ---------------------------------------------------------------- 4. Thống kê người dùng

export async function exportUserStats(rows: UserStatModel[], kindLabel: (n: string | null) => string) {
  const wb = newWorkbook();
  const ws = wb.addWorksheet('Thống kê người dùng', { properties: { tabColor: { argb: `FF${GROUP_COLOR.info.dark}` } } });
  writeTable<UserStatModel>(ws, {
    title: 'THỐNG KÊ NGƯỜI DÙNG – SIGNAI',
    subtitle: `Số liệu tính từ trước tới nay · ${rows.length.toLocaleString('vi-VN')} người dùng · Xuất lúc ${nowText()}`,
    rows,
    totals: true,
    groups: [
      {
        label: 'Thông tin người dùng',
        group: 'info',
        columns: [
          { header: 'Họ tên', width: 26, value: (u) => u.fullName },
          { header: 'Email', width: 32, value: (u) => u.email },
          { header: 'Loại tài khoản', width: 15, value: (u) => kindLabel(u.accountKind) },
          { header: 'Premium', width: 10, value: (u) => (u.premium ? 'Có' : '') },
          { header: 'Ngày đăng ký', width: 14, value: (u) => toDate(u.createdAt), fmt: 'date' },
        ],
      },
      {
        label: 'Học tập',
        group: 'learn',
        columns: [
          { header: 'Thời gian học (phút)', width: 14, value: (u) => u.minutesStudied, fmt: 'int', sum: true },
          { header: 'Số ngày học', width: 11, value: (u) => u.studyDays, fmt: 'int', sum: true },
          { header: 'Bài học hoàn thành', width: 13, value: (u) => u.lessonsCompleted, fmt: 'int', sum: true },
          { header: 'Lượt ôn từ', width: 11, value: (u) => u.signsReviewed, fmt: 'int', sum: true },
          { header: 'Bài kiểm tra đã làm', width: 13, value: (u) => u.quizzesTaken, fmt: 'int', sum: true },
          { header: 'Lượt chấm điểm AI', width: 13, value: (u) => u.aiChecksDone, fmt: 'int', sum: true },
          { header: 'Ngày học gần nhất', width: 14, value: (u) => (u.lastStudyDate ? isoDay(u.lastStudyDate) : null), fmt: 'date' },
        ],
      },
      {
        label: 'Khoá học',
        group: 'course',
        columns: [
          { header: 'Khoá học đã hoàn thành', width: 14, value: (u) => u.coursesCompleted, fmt: 'int', sum: true },
          { header: 'Khoá học đang học', width: 13, value: (u) => u.coursesInProgress, fmt: 'int', sum: true },
        ],
      },
      {
        label: 'Truy cập',
        group: 'access',
        columns: [
          { header: 'Lượt truy cập trang', width: 13, value: (u) => u.pageViews, fmt: 'int', sum: true },
          { header: 'Số phiên', width: 10, value: (u) => u.sessions, fmt: 'int', sum: true },
          { header: 'Lượt vào Góc trò chơi', width: 13, value: (u) => u.gameCornerVisits, fmt: 'int', sum: true },
          { header: 'Đăng nhập gần nhất', width: 17, value: (u) => toDate(u.lastLoginAt), fmt: 'datetime' },
          { header: 'Truy cập gần nhất', width: 17, value: (u) => toDate(u.lastVisitAt), fmt: 'datetime' },
        ],
      },
    ],
  });

  // Sheet giải thích cách tính — người nhận file không phải mở CMS để hỏi
  const notes = wb.addWorksheet('Giải thích', { properties: { tabColor: { argb: 'FF7F7F7F' } } });
  type N = { group: string; name: string; desc: string };
  const N_ROWS: N[] = [
    { group: 'Học tập', name: 'Thời gian học (phút)', desc: 'Tổng số phút học ghi nhận theo ngày (bài học, ôn từ, kiểm tra, chấm AI, trò chơi).' },
    { group: 'Học tập', name: 'Số ngày học', desc: 'Số ngày có ít nhất một hoạt động học.' },
    { group: 'Học tập', name: 'Bài học hoàn thành', desc: 'Số bài học đã học tới nội dung cuối cùng.' },
    { group: 'Học tập', name: 'Lượt ôn từ', desc: 'Số thẻ đã ôn ở mục Ôn từ.' },
    { group: 'Học tập', name: 'Bài kiểm tra đã làm', desc: 'Số lượt kiểm tra đã nộp bài (không tính lượt bỏ dở).' },
    { group: 'Học tập', name: 'Lượt chấm điểm AI', desc: 'Số lần dùng camera để AI chấm ký hiệu.' },
    { group: 'Khoá học', name: 'Khoá học đã hoàn thành', desc: 'Khoá mà người dùng đã hoàn thành mọi bài học đang xuất bản.' },
    { group: 'Khoá học', name: 'Khoá học đang học', desc: 'Khoá đã hoàn thành ít nhất một bài nhưng chưa xong hết.' },
    { group: 'Truy cập', name: 'Lượt truy cập trang', desc: 'Số lần mở trang trên web học khi đang đăng nhập.' },
    { group: 'Truy cập', name: 'Số phiên', desc: 'Số lần vào web; nghỉ quá 30 phút thì tính là phiên mới.' },
    { group: 'Truy cập', name: 'Lượt vào Góc trò chơi', desc: 'Số lần mở trang Góc trò chơi.' },
  ];
  writeTable<N>(notes, {
    title: 'CÁCH TÍNH CÁC CHỈ SỐ',
    rows: N_ROWS,
    groups: [
      {
        label: 'Giải thích',
        group: 'info',
        columns: [
          { header: 'Nhóm', width: 14, value: (x) => x.group },
          { header: 'Chỉ số', width: 26, value: (x) => x.name },
          { header: 'Cách tính', width: 80, value: (x) => x.desc },
        ],
      },
    ],
  });

  await save(wb, 'thong-ke-nguoi-dung');
}
