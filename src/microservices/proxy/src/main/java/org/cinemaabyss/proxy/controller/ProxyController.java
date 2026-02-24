package org.cinemaabyss.proxy.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.cinemaabyss.proxy.config.ProxyProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@Slf4j
public class ProxyController {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade", "host", "content-length"
    );

    private final RestTemplate restTemplate;
    private final ProxyProperties props;

    public ProxyController(RestTemplate restTemplate, ProxyProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/api/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {

        String path = request.getRequestURI();           // /api/...
        String query = request.getQueryString();         // a=1&b=2
        String targetBaseUrl = chooseTargetBaseUrl(path);

        log.info("Proxy routing: {} {} -> {}",
                request.getMethod(),
                path,
                targetBaseUrl);

        String url = targetBaseUrl + path + (query != null ? "?" + query : "");

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpHeaders headers = extractHeaders(request);

        byte[] body = StreamUtils.copyToByteArray(request.getInputStream());

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> resp = restTemplate.exchange(URI.create(url), method, entity, byte[].class);

        // возвращаем статус + заголовки + тело
        HttpHeaders responseHeaders = new HttpHeaders();
        resp.getHeaders().forEach((k, v) -> {
            if (!isHopByHop(k)) responseHeaders.put(k, v);
        });

        return new ResponseEntity<>(resp.getBody(), responseHeaders, resp.getStatusCode());
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (isHopByHop(name)) continue;

            Enumeration<String> values = request.getHeaders(name);
            List<String> list = new ArrayList<>();
            while (values.hasMoreElements()) list.add(values.nextElement());

            headers.put(name, list);
        }
        return headers;
    }

    private boolean isHopByHop(String headerName) {
        return HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private String chooseTargetBaseUrl(String path) {

        if (path.startsWith("/api/events")) {
            return props.eventsServiceUrl();
        }

        if (path.startsWith("/api/movies")) {

            if (!props.gradualMigration()) {
                return props.monolithUrl();
            }

            int percent = clamp(props.moviesMigrationPercent(), 0, 100);
            if (percent == 0) return props.monolithUrl();
            if (percent == 100) return props.moviesServiceUrl();

            int roll = ThreadLocalRandom.current().nextInt(1, 101);
            return (roll <= percent) ? props.moviesServiceUrl() : props.monolithUrl();
        }

        return props.monolithUrl();
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
