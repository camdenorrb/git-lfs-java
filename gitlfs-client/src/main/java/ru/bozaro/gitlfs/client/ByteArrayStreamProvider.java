package ru.bozaro.gitlfs.client;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Create stream by bytes.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
public class ByteArrayStreamProvider implements StreamProvider {
  @NotNull
  private final byte[] data;

  public ByteArrayStreamProvider(@NotNull byte[] data) {
    this.data = data;
  }

  @NotNull
  @Override
  public InputStream getStream() throws IOException {
    return new ByteArrayInputStream(data);
  }
}
