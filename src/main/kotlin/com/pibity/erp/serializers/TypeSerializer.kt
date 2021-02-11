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
  json.addProperty("orgId", type.organization.id)
  json.addProperty("typeName", type.name)
  json.addProperty("autoId", type.autoId)
  val jsonKeys = JsonObject()
  for (key in type.keys)
    jsonKeys.add(key.name, serialize(key))
  json.add("keys", jsonKeys)
  json.add("uniqueConstraints", serialize(type.uniqueConstraints))
  val jsonUniquenessConstraints = JsonObject()
  for (typeUniqueness in type.uniqueConstraints) {
    jsonUniquenessConstraints.add(typeUniqueness.name, JsonArray().apply {
      typeUniqueness.keyUniquenessConstraints.forEach { add(it.key.name) }
    })
  }
  json.add("uniqueConstraints", jsonUniquenessConstraints)
  json.add("permissions", serialize(type.permissions))
  return json
}

fun serialize(entities: Set<Type>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
