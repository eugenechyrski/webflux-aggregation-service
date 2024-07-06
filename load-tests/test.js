import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

export const errorRate = new Rate('errors');
export let options = {
  vus: 1,
  iterations: 1000,
};
export default function () {
let responses = http.batch([
        ['GET', 'http://localhost:8080/aggregation?pricing=NL'],
        ['GET', 'http://localhost:8080/aggregation?pricing=NL'],
        ['GET', 'http://localhost:8080/aggregation?pricing=NL'],
        ['GET', 'http://localhost:8080/aggregation?pricing=NL'],
        ['GET', 'http://localhost:8080/aggregation?pricing=NL'],

    ])



  check(responses[0], {
    'status is 200': (r) => r.status == 200,
  }) || errorRate.add(1);
    check(responses[1], {
    'status is 200': (r) => r.status == 200,
  }) || errorRate.add(1);
    check(responses[2], {
    'status is 200': (r) => r.status == 200,
  }) || errorRate.add(1);
    check(responses[3], {
    'status is 200': (r) => r.status == 200,
  }) || errorRate.add(1);

    check(responses[4], {
    'status is 200': (r) => r.status == 200,
  }) || errorRate.add(1);


}