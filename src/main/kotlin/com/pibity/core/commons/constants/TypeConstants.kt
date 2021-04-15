/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.commons.constants

val primitiveTypes = setOf(
  TypeConstants.TEXT,
  TypeConstants.NUMBER,
  TypeConstants.DECIMAL,
  TypeConstants.BOOLEAN,
  TypeConstants.DATE,
  TypeConstants.TIMESTAMP,
  TypeConstants.TIME,
  TypeConstants.BLOB
)

object TypeConstants {
  const val TEXT = "Text"
  const val NUMBER = "Number"
  const val DECIMAL = "Decimal"
  const val BOOLEAN = "Boolean"
  const val FORMULA = "Formula"
  const val DATE = "Date"
  const val TIMESTAMP = "Timestamp"
  const val TIME = "Time"
  const val BLOB = "Blob"
}
