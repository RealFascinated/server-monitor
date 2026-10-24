package cc.fascinated.monitor.web.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestRequestFilterTest {
    private static final String INGEST_PATH = "/v1/servers/ingest";

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
        }
        return out.toByteArray();
    }

    private static IngestRequestFilter filter(long limitBytes) {
        MonitorIngestProperties properties = new MonitorIngestProperties();
        properties.setMaxRequestSize(DataSize.ofBytes(limitBytes));
        return new IngestRequestFilter(properties);
    }

    private static MockHttpServletRequest post(String path, byte[] body, String encoding) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRequestURI(path);
        request.setContent(body);
        if (encoding != null) {
            request.addHeader("Content-Encoding", encoding);
        }
        return request;
    }

    private record Captured(byte[] body, long payloadBytes, String contentEncoding, long contentLength) {}

    private static Captured run(IngestRequestFilter filter, MockHttpServletRequest request, MockHttpServletResponse response)
            throws Exception {
        AtomicReference<Captured> captured = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
            @Override
            protected void doPost(HttpServletRequest req, jakarta.servlet.http.HttpServletResponse resp) throws IOException {
                byte[] body = req.getInputStream().readAllBytes();
                captured.set(new Captured(body, IngestRequestFilter.payloadBytes(req),
                        req.getHeader("Content-Encoding"), req.getContentLengthLong()));
            }
        });
        filter.doFilter(request, response, chain);
        return captured.get();
    }

    @Test
    void advertisesFeaturesOnHeartbeat() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(filter(1024), post("/v1/servers/heartbeat", new byte[0], null), response);
        assertEquals(IngestRequestFilter.FEATURES, response.getHeader(IngestRequestFilter.FEATURES_HEADER));
    }

    @Test
    void decodesGzipBodyAndCountsDecodedBytes() throws Exception {
        byte[] json = "{\"agentVersion\":\"1.0.0\"}".repeat(50).getBytes(StandardCharsets.UTF_8);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Captured captured = run(filter(1 << 20), post(INGEST_PATH, gzip(json), "gzip"), response);

        assertArrayEquals(json, captured.body());
        assertEquals(json.length, captured.payloadBytes());
        assertNull(captured.contentEncoding(), "the decoded body must not still claim to be encoded");
        assertEquals(-1, captured.contentLength());
    }

    @Test
    void passesPlainBodyThroughAndCountsIt() throws Exception {
        byte[] json = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        Captured captured = run(filter(1024), post(INGEST_PATH, json, null), new MockHttpServletResponse());

        assertArrayEquals(json, captured.body());
        assertEquals(json.length, captured.payloadBytes());
    }

    @Test
    void acceptsIdentityEncoding() throws Exception {
        byte[] json = "{}".getBytes(StandardCharsets.UTF_8);
        Captured captured = run(filter(1024), post(INGEST_PATH, json, "identity"), new MockHttpServletResponse());
        assertArrayEquals(json, captured.body());
    }

    @Test
    void rejectsUnsupportedContentEncoding() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Captured captured = run(filter(1024), post(INGEST_PATH, new byte[]{1}, "br"), response);

        assertEquals(415, response.getStatus());
        assertNull(captured, "the request must not reach the controller");
    }

    @Test
    void capsDecodedSizeNotCompressedSize() throws Exception {
        // 1 MiB of zeros compresses to ~1 KiB, so only a decoded-size limit catches this.
        byte[] bomb = new byte[1 << 20];
        byte[] compressed = gzip(bomb);
        assertTrue(compressed.length < 4096);

        assertThrows(PayloadTooLargeException.class,
                () -> run(filter(64 * 1024), post(INGEST_PATH, compressed, "gzip"), new MockHttpServletResponse()));
    }

    @Test
    void capsPlainBodySize() {
        assertThrows(PayloadTooLargeException.class,
                () -> run(filter(10), post(INGEST_PATH, new byte[100], null), new MockHttpServletResponse()));
    }

    @Test
    void malformedGzipFailsWithIoException() {
        assertThrows(IOException.class,
                () -> run(filter(1024), post(INGEST_PATH, "not gzip".getBytes(StandardCharsets.UTF_8), "gzip"),
                        new MockHttpServletResponse()));
    }

    @Test
    void ignoresOtherPaths() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        run(filter(1024), post("/v1/servers/1/ingest-token/rotate", new byte[0], null), response);
        assertNull(response.getHeader(IngestRequestFilter.FEATURES_HEADER));
    }
}
