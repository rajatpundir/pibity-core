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
import com.pibity.erp.entities.User
import com.pibity.erp.serializers.mappings.serialize

fun serialize(user: User): JsonObject {
  val json = JsonObject()
  json.addProperty("organization", user.id.organization.id)
  json.addProperty("username", user.id.username)
  json.add("groups", serialize(user.userGroups))
  json.add("roles", serialize(user.userRoles))
  return json
}

fun serialize(entities: Set<User>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
