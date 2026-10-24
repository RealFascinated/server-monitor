package cc.fascinated.monitor.web.ingest;

import cc.fascinated.monitor.config.MonitorIngestProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Sits in front of the agent endpoints. It advertises what this API supports to agents, decodes
 * gzip request bodies (the servlet container does not), and caps the decoded body size.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class IngestRequestFilter extends OncePerRequestFilter {
    public static final String FEATURES_HEADER = "X-Monitor-Features";
    public static final String FEATURES = "gzip-ingest,batch-ingest";

    private static final String PAYLOAD_BYTES_ATTRIBUTE = IngestRequestFilter.class.getName() + ".payloadBytes";
    private static final String HEARTBEAT_PATH = "/v1/servers/heartbeat";
    private static final String INGEST_PATH = "/v1/servers/ingest";

    private final MonitorIngestProperties properties;

    public IngestRequestFilter(MonitorIngestProperties properties) {
        this.properties = properties;
    }

    /**
     * Size of the (decoded) request body that the application read, or the declared length if
     * the body was not routed through this filter.
     */
    public static long payloadBytes(HttpServletRequest request) {
        if (request.getAttribute(PAYLOAD_BYTES_ATTRIBUTE) instanceof IngestRequestBody body) {
            return body.bytesRead();
        }
        return Math.max(0, request.getContentLengthLong());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(HEARTBEAT_PATH.equals(path) || INGEST_PATH.equals(path) || path.startsWith(INGEST_PATH + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader(FEATURES_HEADER, FEATURES);

        String encoding = request.getHeader("Content-Encoding");
        boolean gzip = false;
        if (encoding != null && !encoding.isBlank()) {
            String normalized = encoding.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("gzip")) {
                gzip = true;
            } else if (!normalized.equals("identity")) {
                response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Content-Encoding");
                return;
            }
        }

        filterChain.doFilter(new BodyLimitingRequest(request, gzip, this.properties.getMaxRequestSize().toBytes()), response);
    }

    private static final class BodyLimitingRequest extends HttpServletRequestWrapper {
        private final boolean gzip;
        private final long limitBytes;
        private IngestRequestBody body;

        BodyLimitingRequest(HttpServletRequest request, boolean gzip, long limitBytes) {
            super(request);
            this.gzip = gzip;
            this.limitBytes = limitBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (this.body == null) {
                var raw = super.getInputStream();
                this.body = new IngestRequestBody(this.gzip ? new GZIPInputStream(raw) : raw, this.limitBytes);
                setAttribute(PAYLOAD_BYTES_ATTRIBUTE, this.body);
            }
            return this.body;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String name = getCharacterEncoding();
            Charset charset = name == null ? StandardCharsets.UTF_8 : Charset.forName(name);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }

        // The body handed to the application is already decoded, so hide the encoding and length.
        @Override
        public int getContentLength() {
            return this.gzip ? -1 : super.getContentLength();
        }

        @Override
        public long getContentLengthLong() {
            return this.gzip ? -1 : super.getContentLengthLong();
        }

        @Override
        public String getHeader(String name) {
            return hidden(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return hidden(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames()).stream()
                    .filter(name -> !hidden(name))
                    .toList();
            return Collections.enumeration(names);
        }

        private boolean hidden(String name) {
            return this.gzip && ("Content-Encoding".equalsIgnoreCase(name) || "Content-Length".equalsIgnoreCase(name));
        }
    }
}
