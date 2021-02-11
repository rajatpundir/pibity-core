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
import com.pibity.erp.entities.uniqueness.TypeUniqueness

fun serialize(typeUniqueness: TypeUniqueness): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", typeUniqueness.type.organization.id)
  json.addProperty("typeName", typeUniqueness.type.name)
  json.addProperty("constraintName", typeUniqueness.name)
  val jsonKeys = JsonArray()
  for (keyUniqueness in typeUniqueness.keyUniquenessConstraints)
    jsonKeys.add(keyUniqueness.key.name)
  json.add("keys", jsonKeys)
  return json
}

fun serialize(entities: Set<TypeUniqueness>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
