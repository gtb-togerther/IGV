/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.broad.igv.util.stream;

import com.google.common.primitives.Ints;
import net.sf.samtools.seekablestream.SeekableStream;

import java.io.IOException;

import static java.lang.System.arraycopy;

/**
 * A wrapper class to provide buffered read access to a SeekableStream.  Just wrapping such a stream with
 * a BufferedInputStream will not work as it does not support seeking.  In this implementation a
 * seek call is delegated to the wrapped stream, and the buffer reset.
 */
public class IGVSeekableBufferedStream extends SeekableStream {

    public static final int DEFAULT_BUFFER_SIZE = 512000;

    final private int maxBufferSize;
    final SeekableStream wrappedStream;
    long position;
    long length;

    int markpos;
    int marklimit;

    byte[] buffer;
    long bufferStartPosition; // Position in file corresponding to start of buffer
    int bufferSize;

    public IGVSeekableBufferedStream(final SeekableStream stream, final int bsize) {
        this.maxBufferSize = bsize;
        this.wrappedStream = stream;
        this.position = 0;
        this.length = wrappedStream.length();
        this.buffer = new byte[maxBufferSize];
        this.bufferStartPosition = -1;
        this.bufferSize = 0;

    }

    public IGVSeekableBufferedStream(final SeekableStream stream) {
        this(stream, DEFAULT_BUFFER_SIZE);
    }

    public long length() {
        return length;
    }

    @Override
    public long skip(final long skipLength) throws IOException {
        long actualSkip = Math.min(length - position - 1, skipLength);
        position += actualSkip;
        return actualSkip;
    }

    @Override
    public synchronized void reset() throws IOException {

        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        }
        position = markpos;

    }

    @Override
    public synchronized void mark(int readlimit) {
        this.markpos = (int) position;
        this.marklimit = readlimit;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    public void seek(final long position) throws IOException {
        this.position = position;
    }

    public void close() throws IOException {
        wrappedStream.close();
    }

    public boolean eof() throws IOException {
        return position >= wrappedStream.length();
    }

    @Override
    public String getSource() {
        return wrappedStream.getSource();
    }

    @Override
    public long position() throws IOException {
        return position;
    }

    /**
     * Return true iff the buffer needs to be refilled for the given
     * amount of data requested
     * @param len Number of bytes from {@code position} one plans on reading
     * @return
     */
    private boolean needFillBuffer(int len){
        return bufferSize == 0 || position < bufferStartPosition || (position + len) > bufferStartPosition + bufferSize;
    }


    public int read() throws IOException {

        if (needFillBuffer(1)) {
            fillBuffer();
        }

        int offset = (int) (position - bufferStartPosition);
        int b = buffer[offset];
        position++;
        return b;
    }

    /**
     * This method implements the general contract of the corresponding read method of the InputStream class.
     * @param b   destination buffer;
     * @param off offset at which to start storing bytes.
     * @param len maximum number of bytes to read.
     * @return the number of bytes read, or -1 if the end of the stream has been reached.
     * @throws IOException
     */
    public int read(final byte[] b, final int off, final int len) throws IOException {

        if (position >= length) return -1;

        if (len > maxBufferSize) {
            // Buffering not useful here.  Don't bother trying to use any (possible) overlapping buffer contents
            wrappedStream.seek(position);
            int nBytes = wrappedStream.read(b, off, len);
            position += nBytes;
            return nBytes;
        } else {
            // Requested range is not contained within buffer.
            if (needFillBuffer(len)) {
                fillBuffer();
            }

            int bufferOffset = (int) (position - bufferStartPosition);
            int bytesCopied = Math.min(len, bufferSize - bufferOffset);
            arraycopy(buffer, bufferOffset, b, off, bytesCopied);
            position += bytesCopied;
            return bytesCopied;
        }
    }

    private void fillBuffer() throws IOException {

        int n = 0;
        long longLen = Math.min((long) maxBufferSize, (length - position));
        //This shouldn't actually be necessary as long as maxBufferSize is
        //an int, but we leave it here to stress the fact that
        //we need to watch for overflow
        int len = Ints.saturatedCast(longLen);

        int offset = 0;
//        long bufferEnd = bufferStartPosition + bufferSize;
//        if(position + len > bufferStartPosition && position < bufferStartPosition) {
//           // There is some overlap
//            int sz = (int) (position + len - bufferStartPosition);
//            arraycopy(buffer, bufferSize - sz, buffer, 0, sz);
//
//            n += sz;
//            len -= sz;
//        }
//        else if(position + len > bufferEnd && position < bufferEnd) {
//            int sz = (int) (bufferEnd - position);
//            offset = sz;
//            arraycopy(buffer, 0, buffer, bufferSize - sz, sz);
//
//            n += sz;
//            len -= sz;
//
//        }

        if (len > 0) {
            wrappedStream.seek(position + offset);
            while (n < len) {
                int count = wrappedStream.read(buffer, n, len - n);
                if (count < 0) {
                    break;  // EOF.  This should not be possible as len is capped above.
                }
                n += count;
            }
            bufferStartPosition = position;
            bufferSize = n;
        }
    }
}