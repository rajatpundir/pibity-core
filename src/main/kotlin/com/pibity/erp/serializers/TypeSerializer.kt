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
import com.pibity.erp.entities.Type

fun serialize(type: Type): JsonObject {
  val json = JsonObject()
  if (type.superTypeName == "Any") {
    json.addProperty("orgId", type.organization.id)
    json.addProperty("autoId", type.autoAssignId)
  }
  else
    json.addProperty("superTypeName", type.superTypeName)
  json.addProperty("typeName", type.name)
//  json.addProperty("version", type.version.time)
  val jsonKeys = JsonObject()
  for (key in type.keys)
    jsonKeys.add(key.name, serialize(key))
  json.add("keys", jsonKeys)
  if (type.superTypeName == "Any")
    json.add("permissions", serialize(type.permissions))
  return json
}

fun serialize(entities: Set<Type>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
