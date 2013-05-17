/*
 * Copyright 2013 eXo Platform SAS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package juzu.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public abstract class BinaryStream implements Stream {

  /** . */
  public static final int BUFFER_SIZE = 512;

  /** Charset. */
  private Charset charset;

  /** Encoder. */
  private CharsetEncoder encoder;

  /** . */
  private ByteBuffer bb;

  /** The cached buffer for single char conversion. */
  private CharBuffer single;

  protected BinaryStream(Charset charset) {
    this.charset = charset;
  }

  public abstract BinaryStream append(byte[] data, int off, int len) throws IOException;

  public abstract BinaryStream append(byte[] data) throws IOException;

  public Stream append(CharSequence csq) throws IOException {
    return append(csq, 0, csq.length());
  }

  public Stream append(CharSequence csq, int start, int end) throws IOException {
    return encode(CharBuffer.wrap(csq, start, end));
  }

  public Stream append(char c) throws IOException {
    if (single == null) {
      single = CharBuffer.allocate(1);
    } else {
      single.compact();
    }
    single.put(c);
    single.flip();
    return encode(single);
  }

  private Stream encode(CharBuffer buffer) throws IOException {
    if (buffer.hasRemaining()) {
      if (encoder == null) {
        encoder = charset.newEncoder().onUnmappableCharacter(CodingErrorAction.REPORT).onMalformedInput(CodingErrorAction.IGNORE);
        bb = ByteBuffer.allocate(BUFFER_SIZE);
      } else {
        encoder.reset();
      }
      while (true) {
        CoderResult result ;
        result = buffer.hasRemaining() ? encoder.encode(buffer, bb, true) : encoder.flush(bb);
        if (result.isUnderflow() || result.isOverflow()) {
          bb.flip();
          if (bb.hasRemaining()) {
            append(bb.array(), bb.arrayOffset() + bb.position(), bb.limit() - bb.arrayOffset());
          }
          bb.clear();
          if (result.isUnderflow()) {
            if (buffer.remaining() > 0) {
              throw new UnsupportedOperationException("We don't support this case yet");
            } else {
              break;
            }
          }
        } else {
          if (result.isUnmappable()) {
            buffer.position(buffer.position() + result.length());
          } else {
            throw new UnsupportedOperationException("We don't support this case yet (2) " + result);
          }
        }
      }
    }
    return this;
  }
}
