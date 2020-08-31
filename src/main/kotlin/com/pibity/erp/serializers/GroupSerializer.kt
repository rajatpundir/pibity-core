/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.entities.Group
import com.pibity.erp.serializers.mappings.serialize

fun serialize(src: Group): JsonObject {
  val json = JsonObject()
  json.addProperty("organization", src.id.organization.id)
  json.addProperty("groupName", src.id.name)
  json.add("roles", serialize(src.groupRoles))
  return json
}

fun serialize(src: Set<Group>): JsonArray {
  val json = JsonArray()
  for (entity in src)
    json.add(serialize(entity))
  return json
}
