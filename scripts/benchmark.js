// Fixed-rate read workload, so both runs face the same offered load.
import http from 'k6/http';
import { check } from 'k6';

const BASE = 'http://app:8080/api';
const KEYWORDS = ['graphs', 'dynamic', 'recursion', 'databases', 'networks', 'operating', 'algebra', 'probability'];
const TYPES = ['LECTURE_NOTES', 'PAST_EXAM', 'ASSIGNMENT', 'TEXTBOOK', 'VIDEO_LINK'];

const pick = (n) => Math.floor(Math.random() * n);

const REQUESTS = [
  () => `/courses?page=${pick(5)}`,
  () => `/courses/${1 + pick(50)}`,
  () => `/resources/${1 + pick(200)}`,
  () => `/resources/course/${1 + pick(50)}`,
  () => `/resources/search?q=${KEYWORDS[pick(KEYWORDS.length)]}`,
  () => `/resources/search?courseId=${1 + pick(50)}&type=${TYPES[pick(TYPES.length)]}`,
];

export const options = {
  discardResponseBodies: true,
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)'],
  scenarios: {
    reads: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 100),
      timeUnit: '1s',
      duration: __ENV.DURATION || '60s',
      preAllocatedVUs: 50,
      maxVUs: 300,
    },
  },
};

export default function () {
  const res = http.get(BASE + REQUESTS[pick(REQUESTS.length)]());
  check(res, { 'status is 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  const d = data.metrics.http_req_duration.values;
  return {
    stdout: JSON.stringify({
      requests: data.metrics.http_reqs.values.count,
      failed: data.metrics.http_req_failed.values.rate,
      avg: d.avg,
      p50: d.med,
      p95: d['p(95)'],
      p99: d['p(99)'],
    }) + '\n',
  };
}
