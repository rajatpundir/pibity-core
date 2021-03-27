/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.entities.Type

fun serialize(type: Type): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, type.organization.id)
  addProperty(OrganizationConstants.TYPE_NAME, type.name)
  addProperty("autoId", type.autoId)
  add("keys", type.keys.fold(JsonObject()) { acc, key -> acc.apply { add(key.name, serialize(key)) } })
  add("uniqueConstraints", serialize(type.uniqueConstraints))
  add("assertions", serialize(type.typeAssertions))
  add("permissions", serialize(type.permissions))
}

fun serialize(entities: Set<Type>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
