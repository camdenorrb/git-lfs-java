package ru.bozaro.gitlfs.server;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.bozaro.gitlfs.common.Constants;
import ru.bozaro.gitlfs.common.data.*;
import ru.bozaro.gitlfs.common.data.Error;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Pointer manager for local ContentManager.
 *
 * @author Artem V. Navrotskiy <bozaro@users.noreply.github.com>
 */
public class LocalPointerManager implements PointerManager {
  @NotNull
  private final ContentManager manager;
  @NotNull
  private final String contentLocation;

  private interface AccessChecker {
    @NotNull
    Object checkAccess(@NotNull HttpServletRequest request) throws IOException, ForbiddenError, UnauthorizedError;
  }

  /**
   * Create pointer manager for local ContentManager.
   *
   * @param manager         Content manager.
   * @param contentLocation Absolute or relative URL to ContentServlet.
   */
  public LocalPointerManager(@NotNull ContentManager manager, @NotNull String contentLocation) {
    this.manager = manager;
    this.contentLocation = contentLocation.endsWith("/") ? contentLocation : contentLocation + "/";
  }

  @NotNull
  @Override
  public Locator checkAccess(@NotNull HttpServletRequest request, @NotNull final URI selfUrl, @NotNull final Operation operation) throws IOException, ForbiddenError, UnauthorizedError {
    operation.visit(new CheckAccessVisitor()).checkAccess(request);

    final String auth = request.getHeader(Constants.HEADER_AUTHORIZATION);
    return new Locator() {
      @NotNull
      @Override
      public BatchItem[] getLocations(@NotNull Meta[] metas) throws IOException {
        final BatchItem[] result = new BatchItem[metas.length];
        final Map<String, String> header = auth != null ? ImmutableMap.of(Constants.HEADER_AUTHORIZATION, auth) : null;
        for (int i = 0; i < metas.length; ++i) {
          result[i] = getLocation(header, selfUrl, metas[i]);
        }
        return result;
      }

      @NotNull
      public BatchItem getLocation(@Nullable Map<String, String> header, @NotNull URI selfUrl, @NotNull Meta meta) throws IOException {
        final Meta storageMeta = manager.getMetadata(meta.getOid());
        if (storageMeta == null) {
          return new BatchItem(meta, ImmutableMap.of(LinkType.Upload, new Link(selfUrl.resolve(contentLocation).resolve(meta.getOid()), header, null)));
        } else if ((meta.getSize() >= 0) && (storageMeta.getSize() != meta.getSize())) {
          return new BatchItem(meta, new Error(422, "Invalid object size"));
        } else {
          return new BatchItem(storageMeta, ImmutableMap.of(LinkType.Download, new Link(selfUrl.resolve(contentLocation).resolve(storageMeta.getOid()), header, null)));
        }
      }
    };
  }

  private class CheckAccessVisitor implements Operation.Visitor<AccessChecker> {
    @Override
    public AccessChecker visitDownload() {
      return new AccessChecker() {
        @NotNull
        @Override
        public Object checkAccess(@NotNull HttpServletRequest request) throws IOException, ForbiddenError, UnauthorizedError {
          return manager.checkDownloadAccess(request);
        }
      };
    }

    @Override
    public AccessChecker visitUpload() {
      return new AccessChecker() {
        @NotNull
        @Override
        public Object checkAccess(@NotNull HttpServletRequest request) throws IOException, ForbiddenError, UnauthorizedError {
          return manager.checkUploadAccess(request);
        }
      };
    }
  }
}
