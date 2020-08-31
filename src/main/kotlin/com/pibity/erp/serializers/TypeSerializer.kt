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

fun serialize(src: Type): JsonObject {
  val json = JsonObject()
  if (src.id.superTypeName == "Any")
    json.addProperty("organizationId", src.id.organization.id)
  else
    json.addProperty("superTypeName", src.id.superTypeName)
  json.addProperty("typeName", src.id.name)
  json.addProperty("displayName", src.displayName)
  val jsonKeys = JsonObject()
  for (key in src.keys)
    jsonKeys.add(key.id.name, serialize(key))
  json.add("keys", jsonKeys)
  if (src.id.superTypeName == "Any")
    json.add("permissions", serialize(src.permissions))
  return json
}

fun serialize(src: Set<Type>): JsonArray {
  val json = JsonArray()
  for (entity in src)
    json.add(serialize(entity))
  return json
}
