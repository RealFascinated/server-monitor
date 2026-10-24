package cc.fascinated.monitor.web.ingest;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Counts the bytes handed to the application and refuses to hand over more than {@code limitBytes}.
 * The delegate may be a decompressing stream, so the count and limit apply to the decoded payload.
 */
final class IngestRequestBody extends ServletInputStream {
    private final InputStream delegate;
    private final long limitBytes;
    private long bytesRead;
    private boolean finished;

    IngestRequestBody(InputStream delegate, long limitBytes) {
        this.delegate = delegate;
        this.limitBytes = limitBytes;
    }

    long bytesRead() {
        return this.bytesRead;
    }

    @Override
    public int read() throws IOException {
        int value = this.delegate.read();
        if (value < 0) {
            this.finished = true;
            return -1;
        }
        count(1);
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = this.delegate.read(buffer, offset, length);
        if (read < 0) {
            this.finished = true;
            return -1;
        }
        count(read);
        return read;
    }

    private void count(int read) throws PayloadTooLargeException {
        this.bytesRead += read;
        if (this.bytesRead > this.limitBytes) {
            throw new PayloadTooLargeException(this.limitBytes);
        }
    }

    @Override
    public void close() throws IOException {
        this.delegate.close();
    }

    @Override
    public boolean isFinished() {
        return this.finished;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException("Asynchronous reads are not supported for ingest requests");
    }
}
