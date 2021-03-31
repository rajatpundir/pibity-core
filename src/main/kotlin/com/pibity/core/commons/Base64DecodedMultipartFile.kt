/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.commons

import org.springframework.web.multipart.MultipartFile
import java.io.*

class Base64DecodedMultipartFile(private val bytes: ByteArray) : MultipartFile {
  override fun getInputStream(): InputStream = bytes.inputStream()

  override fun getName(): String = ""

  override fun getOriginalFilename(): String = ""

  override fun getContentType(): String = ""

  override fun isEmpty(): Boolean = bytes.isEmpty()

  override fun getSize(): Long = bytes.size.toLong()

  override fun getBytes(): ByteArray = bytes

  override fun transferTo(dest: File) {}
}
