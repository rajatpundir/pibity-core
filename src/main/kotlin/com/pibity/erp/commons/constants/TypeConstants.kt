/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.constants

val primitiveTypes = listOf(
    TypeConstants.TEXT,
    TypeConstants.NUMBER,
    TypeConstants.DECIMAL,
    TypeConstants.BOOLEAN,
    TypeConstants.LIST,
    TypeConstants.FORMULA)

val formulaReturnTypes = listOf(
    TypeConstants.TEXT,
    TypeConstants.NUMBER,
    TypeConstants.DECIMAL,
    TypeConstants.BOOLEAN)

object TypeConstants {
  const val TEXT = "Text"
  const val NUMBER = "Number"
  const val DECIMAL = "Decimal"
  const val BOOLEAN = "Boolean"
  const val LIST = "List"
  const val FORMULA = "Formula"
  const val DATE = "Date"
}