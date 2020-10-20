/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.services

import com.google.gson.JsonObject
import org.springframework.stereotype.Service

@Service
class AssertionService(
    val typeService: TypeService
) {

  fun createAssertion(jsonParams: JsonObject): JsonObject {
    return JsonObject()
  }
}
