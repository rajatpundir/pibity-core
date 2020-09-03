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
import com.pibity.erp.entities.Type

fun serialize(type: Type): JsonObject {
  val json = JsonObject()
  if (type.id.superTypeName == "Any")
    json.addProperty("organizationId", type.id.organization.id)
  else
    json.addProperty("superTypeName", type.id.superTypeName)
  json.addProperty("typeName", type.id.name)
  json.addProperty("displayName", type.displayName)
  val jsonKeys = JsonObject()
  for (key in type.keys)
    jsonKeys.add(key.id.name, serialize(key))
  json.add("keys", jsonKeys)
  if (type.id.superTypeName == "Any")
    json.add("permissions", serialize(type.permissions))
  return json
}

fun serialize(entities: Set<Type>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
