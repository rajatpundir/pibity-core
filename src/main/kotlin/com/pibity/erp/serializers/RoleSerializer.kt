/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.entities.Role
import com.pibity.erp.serializers.mappings.serialize

fun serialize(role: Role): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", role.organization.id)
  json.addProperty("roleName", role.name)
//  json.addProperty("version", role.version.time)
  json.add("typePermissions", serialize(role.roleTypePermissions))
  json.add("functionPermissions", serialize(role.roleFunctionPermissions))
  return json
}

fun serialize(entities: Set<Role>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
