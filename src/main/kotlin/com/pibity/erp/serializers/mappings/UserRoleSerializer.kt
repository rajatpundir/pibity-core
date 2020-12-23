/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers.mappings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.entities.mappings.UserRole
import com.pibity.erp.serializers.serialize

fun serialize(userRole: UserRole): JsonObject {
  return serialize(userRole.id.role)
}

fun serialize(entities: Set<UserRole>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
