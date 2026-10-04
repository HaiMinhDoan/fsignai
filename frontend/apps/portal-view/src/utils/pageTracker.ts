import type { RouteLocationNormalized } from 'vue-router';
import { http } from '@/api/http';

/**
 * Báo lượt xem trang về backend để Dashboard CMS có số truy cập.
 *
 * Không dùng cookie theo dõi, không gửi IP hay đường dẫn đầy đủ của trang giới thiệu:
 *  • visitor_id: chuỗi ngẫu nhiên giữ trong localStorage — đếm được khách chưa đăng nhập
 *  • session_id: sinh lại sau 30 phút không chuyển trang (giữ trong sessionStorage)
 *  • referrer: chỉ gửi ở lượt xem ĐẦU phiên và chỉ khi đến từ trang khác; máy chủ chỉ giữ tên miền
 *
 * Lỗi mạng thì bỏ qua lặng lẽ — đo đếm hỏng không bao giờ được làm phiền người học.
 */

const VISITOR_KEY = 'signai_visitor_id';
const SESSION_KEY = 'signai_session';
const SESSION_IDLE_MS = 30 * 60 * 1000;

function randomId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) return crypto.randomUUID();
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
}

function visitorId(): string {
  try {
    let id = localStorage.getItem(VISITOR_KEY);
    if (!id) {
      id = randomId();
      localStorage.setItem(VISITOR_KEY, id);
    }
    return id;
  } catch {
    return memoryVisitor;
  }
}
const memoryVisitor = randomId();

/** Trả [sessionId, có phải lượt xem đầu phiên không] */
function session(): [string, boolean] {
  const now = Date.now();
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    const cu = raw ? (JSON.parse(raw) as { id: string; at: number }) : null;
    const moi = !cu || now - cu.at > SESSION_IDLE_MS;
    const id = moi ? randomId() : cu!.id;
    sessionStorage.setItem(SESSION_KEY, JSON.stringify({ id, at: now }));
    return [id, moi];
  } catch {
    return [memoryVisitor, false];
  }
}

function externalReferrer(): string | undefined {
  try {
    const ref = document.referrer;
    if (!ref) return undefined;
    return new URL(ref).host === location.host ? undefined : ref;
  } catch {
    return undefined;
  }
}

export function trackPageView(to: RouteLocationNormalized) {
  const [sessionId, firstInSession] = session();
  http
    .post('/analytics/page-view', {
      visitorId: visitorId(),
      sessionId,
      path: to.path.slice(0, 300),
      routeName: typeof to.name === 'string' ? to.name : undefined,
      referrer: firstInSession ? externalReferrer() : undefined,
    })
    .catch(() => undefined);
}
