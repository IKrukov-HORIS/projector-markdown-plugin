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
package org.jetbrains.projector.plugins.markdown.injection;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface CodeFenceLanguageProvider {
  ExtensionPointName<CodeFenceLanguageProvider> EP_NAME =
    ExtensionPointName.create("org.jetbrains.projector.markdown.fenceLanguageProvider");

  /**
   * Implement this method to provide custom rule for selecting {@link Language} to inject into the code fences
   *
   * @param infoString the string with "info string" of the code fence. Not trimmed nor lowercased.
   * @return Language which should be injected into the code fence with the given infoString.
   * No custom injection rule is applied if null is returned.
   * @see <a href="http://spec.commonmark.org/0.27/#info-string">Info String</a>
   * @see <a href="http://spec.commonmark.org/0.27/#code-fence">Code Fence</a>
   */
  @Nullable
  Language getLanguageByInfoString(@NotNull String infoString);

  /**
   * Implement this method to provide custom completion variants for info strings in the all fences.
   * Note that a special insertHandler for handling code fence opening will be prepended for your lookup.
   * That means that the custom insert handlers should be ready for uncommitted documents and rely only on editor and document.
   * See {@link LanguageListCompletionContributor#doFillVariants} for the details on implementation.
   *
   * @return A list of {@link LookupElement} which will be prepended to the default language list.
   */
  @NotNull
  List<LookupElement> getCompletionVariantsForInfoString(@NotNull CompletionParameters parameters);
}
