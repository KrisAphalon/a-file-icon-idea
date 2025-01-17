/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 Elior "Mallowigi" Boukhobza
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

package com.mallowigi.icons.providers

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.indexing.FileBasedIndex
import com.mallowigi.icons.associations.Association
import com.mallowigi.icons.associations.Associations
import com.mallowigi.icons.associations.FileAssociationsIndex
import com.mallowigi.models.FileInfo
import com.mallowigi.models.IconType
import com.mallowigi.models.VirtualFileInfo
import javax.swing.Icon

/** Abstract file icon provider. */
abstract class AbstractFileIconProvider : IconProvider(), DumbAware {
  /**
   * Get the icon for the given psiElement
   *
   * @param element The psiElement to get the icon for
   * @param flags The flags (unused)
   */
  override fun getIcon(element: PsiElement, flags: Int): Icon? = when {
    isNotApplicable() -> null
    isOfType(element) -> findIcon(element)
    else              -> null
  }

  /**
   * Find icon for a psiElement
   *
   * @param element the psi element
   * @return icon if found
   */
  private fun findIcon(element: PsiElement): Icon? {
    val virtualFile = PsiUtilCore.getVirtualFile(element)
    return virtualFile?.let {
      val file: FileInfo = VirtualFileInfo(it)
      val association = findAssociation(file, element.project)
      getIconForAssociation(association)
    }
  }

  /** Get icon for association. */
  private fun getIconForAssociation(association: Association?): Icon? = association?.let { loadIcon(it) }

  /** Load icon. */
  private fun loadIcon(association: Association): Icon? =
    CacheIconProvider.instance.iconCache.getOrPut(association.icon) { getIcon(association.icon) }

  /** Finds and retrieves the first matching association for the given file within the specified project scope. */
  private fun findAssociation(file: FileInfo, project: Project): Association? {
    if (getType() == IconType.FOLDER) return getSource().findAssociation(file)
    if (CACHE.containsKey(file.path)) return CACHE[file.path]

    val fileBasedIndex = FileBasedIndex.getInstance()
    val associations = fileBasedIndex.getValues(
      FileAssociationsIndex.NAME,
      file.path,
      GlobalSearchScope.projectScope(project)
    )
    val association = associations.firstOrNull()
    if (association != null) CACHE[file.path] = association
    return association
  }

  /**
   * Checks whether psiElement is of type (PsiFile/PsiDirectory) defined by this provider
   *
   * @param element the psi element
   * @return true if element is of type defined by this provider
   */
  abstract fun isOfType(element: PsiElement): Boolean

  /**
   * Determine whether this provider is applicable
   *
   * @return true if not applicable
   */
  abstract fun isNotApplicable(): Boolean

  /**
   * Get the source of associations
   *
   * @return the [Associations] source
   */
  abstract fun getSource(): Associations

  /**
   * Get icon of an icon path
   *
   * @param iconPath the icon path to check
   * @return icon if there is an [Association] for this path
   */
  abstract fun getIcon(iconPath: String): Icon?

  /** Return the [IconType] of this provider. */
  abstract fun getType(): IconType

  /**
   * Whether this provider is for default associations
   *
   * @return true if default assoc provider
   */
  abstract fun isDefault(): Boolean

  companion object {
    private val CACHE: MutableMap<String, Association> = mutableMapOf()
  }
}
