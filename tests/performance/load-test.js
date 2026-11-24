// Load test with k6 (https://k6.io). Install k6, start QuickCourt, then run:
//     k6 run tests/performance/load-test.js
// Against the cloud:  k6 run -e BASE_URL=https://your-domain tests/performance/load-test.js
// Scenario: users browse venues and check slots (read-heavy traffic), ramping from 1 to 50 virtual users.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '40s', target: 50 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],      // less than 1% errors
    http_req_duration: ['p(95)<800'],    // 95% of requests under 800 ms (ASSUMED target for a free-tier server)
  },
};

export default function () {
  const venues = http.get(`${BASE}/api/venues`);
  check(venues, { 'venues 200': (r) => r.status === 200 });

  const list = venues.json();
  if (list && list.length > 0) {
    const v = list[0];
    const courts = http.get(`${BASE}/api/venues/${v.id}/courts`);
    check(courts, { 'courts 200': (r) => r.status === 200 });
    const c = courts.json();
    if (c && c.length > 0) {
      const d = new Date(Date.now() + 2 * 86400000).toISOString().slice(0, 10);
      const slots = http.get(`${BASE}/api/venues/${v.id}/slots?courtId=${c[0].id}&date=${d}`);
      check(slots, { 'slots 200': (r) => r.status === 200 });
    }
  }
  const health = http.get(`${BASE}/actuator/health`);
  check(health, { 'health UP': (r) => r.status === 200 });
  sleep(1);
}
