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
import com.pibity.erp.commons.utils.gson
import com.pibity.erp.entities.TypeAssertion

fun serialize(typeAssertion: TypeAssertion): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", typeAssertion.type.organization.id)
  json.addProperty("type", typeAssertion.type.name)
  json.addProperty("assertionName", typeAssertion.name)
//  json.addProperty("version", assertion.version.time)
  json.add("assertion", gson.fromJson(typeAssertion.expression, JsonObject::class.java))
  return json
}

fun serialize(entities: Set<TypeAssertion>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
