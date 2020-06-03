/*
 * MIT License
 *
 * Copyright (c) 2019-2020 JetBrains s.r.o.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jetbrains.projector.plugins.markdown.ui.preview;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.projector.plugins.markdown.extensions.MarkdownCodeFenceCacheableProvider;
import org.jetbrains.projector.plugins.markdown.extensions.MarkdownCodeFencePluginGeneratingProvider;
import org.jetbrains.projector.plugins.markdown.lang.MarkdownFileType;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.intellij.util.ArrayUtilRt.EMPTY_FILE_ARRAY;

public class MarkdownCodeFencePluginCache implements Disposable {
  @NotNull public static final String MARKDOWN_FILE_PATH_KEY = "markdown-md5-file-path";

  @NotNull private final Alarm myAlarm = new Alarm(this);
  @NotNull private final Collection<MarkdownCodeFencePluginCacheCollector> myCodeFencePluginCaches = ContainerUtil.newConcurrentSet();
  @NotNull private final Collection<File> myAdditionalCacheToDelete = ContainerUtil.newConcurrentSet();

  public MarkdownCodeFencePluginCache() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      scheduleClearCache();
    }

    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        if (FileTypeRegistry.getInstance().isFileOfType(event.getFile(), MarkdownFileType.INSTANCE)) {
          myAdditionalCacheToDelete.addAll(processSourceFileToDelete(event.getFile(), ContainerUtil.emptyList()));
        }
      }
    });
  }

  public Collection<File> collectFilesToRemove() {
    return myCodeFencePluginCaches.stream()
      .flatMap(cacheProvider -> processSourceFileToDelete(cacheProvider.getFile(), cacheProvider.getAliveCachedFiles()).stream())
      .collect(Collectors.toList());
  }

  public void registerCacheProvider(@NotNull MarkdownCodeFencePluginCacheCollector cacheCollector) {
    myCodeFencePluginCaches.add(cacheCollector);
  }

  private void scheduleClearCache() {
    myAlarm.addRequest(() -> {
      Collection<File> filesToDelete = ContainerUtil.union(myAdditionalCacheToDelete, collectFilesToRemove());
      FileUtil.asyncDelete(filesToDelete);

      clear();

      scheduleClearCache();
    }, Registry.intValue("markdown.clear.cache.interval"));
  }

  private void clear() {
    myAdditionalCacheToDelete.clear();
    myCodeFencePluginCaches.clear();
  }

  @Override
  public void dispose() {
    Disposer.dispose(myAlarm);
  }

  public static MarkdownCodeFencePluginCache getInstance() {
    return ServiceManager.getService(MarkdownCodeFencePluginCache.class);
  }

  private static List<File> getPluginSystemPaths() {
    return Arrays.stream(MarkdownCodeFencePluginGeneratingProvider.Companion.getEP_NAME().getExtensions())
      .filter(MarkdownCodeFenceCacheableProvider.class::isInstance)
      .map(MarkdownCodeFenceCacheableProvider.class::cast)
      .map(provider -> new File(provider.getCacheRootPath()))
      .collect(Collectors.toList());
  }

  private static Collection<File> processSourceFileToDelete(@NotNull VirtualFile sourceFile, @NotNull Collection<File> aliveCachedFiles) {
    Collection<File> filesToDelete = new HashSet<>();
    for (File codeFencePluginSystemPath : getPluginSystemPaths()) {
      for (File sourceFileCacheDirectory : getChildren(codeFencePluginSystemPath)) {
        if (isCachedSourceFile(sourceFileCacheDirectory, sourceFile) && aliveCachedFiles.isEmpty()) {
          filesToDelete.add(sourceFileCacheDirectory);
          continue;
        }

        for (File imgFile : getChildren(sourceFileCacheDirectory)) {
          if (!isCachedSourceFile(sourceFileCacheDirectory, sourceFile) || aliveCachedFiles.contains(imgFile)) continue;

          filesToDelete.add(imgFile);
        }
      }
    }

    return filesToDelete;
  }

  private static File[] getChildren(@NotNull File directory) {
    File[] files = directory.listFiles();
    return files != null ? files : EMPTY_FILE_ARRAY;
  }

  private static boolean isCachedSourceFile(@NotNull File sourceFileDir, @NotNull VirtualFile sourceFile) {
    return sourceFileDir.getName().equals(MarkdownUtil.INSTANCE.md5(sourceFile.getPath(), MARKDOWN_FILE_PATH_KEY));
  }
}
