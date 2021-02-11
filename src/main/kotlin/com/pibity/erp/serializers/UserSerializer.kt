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
import com.pibity.erp.entities.User
import com.pibity.erp.serializers.mappings.serialize

fun serialize(user: User): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", user.organization.id)
  json.addProperty("username", user.username)
  json.addProperty("active", user.active)
  json.addProperty("email", user.email)
  json.addProperty("firstName", user.firstName)
  json.addProperty("lastName", user.lastName)
  json.add("details", serialize(user.details!!))
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
