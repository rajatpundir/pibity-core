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
import com.pibity.erp.entities.permission.FunctionPermission

fun serialize(functionPermission: FunctionPermission): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", functionPermission.function.organization.id)
  json.addProperty("functionName", functionPermission.function.name)
  json.addProperty("permissionName", functionPermission.name)
  json.add("permissions", JsonObject().apply {
    add("inputs", JsonObject().apply {
      functionPermission.functionInputPermissions.forEach { addProperty(it.functionInput.name, it.accessLevel) }
    })
    add("outputs", JsonObject().apply {
      functionPermission.functionOutputPermissions.forEach { addProperty(it.functionOutput.name, it.accessLevel) }
    })
  })
  return json
}

fun serialize(entities: Set<FunctionPermission>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
