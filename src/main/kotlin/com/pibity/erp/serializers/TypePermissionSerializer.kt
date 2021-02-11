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
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.entities.permission.TypePermission

fun serialize(typePermission: TypePermission): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", typePermission.type.organization.id)
  json.addProperty("typeName", typePermission.type.name)
  json.addProperty("permissionName", typePermission.name)
  json.addProperty("creatable", typePermission.creatable)
  json.addProperty("deletable", typePermission.deletable)
  val jsonKeyPermissions = JsonObject()
  for (keyPermission in typePermission.keyPermissions) {
    jsonKeyPermissions.addProperty(
      keyPermission.key.name,
      when (keyPermission.accessLevel) {
        PermissionConstants.READ_ACCESS -> "READ"
        PermissionConstants.WRITE_ACCESS -> "WRITE"
        else -> "NONE"
      }
    )
  }
  json.add("permissions", jsonKeyPermissions)
  return json
}

fun serialize(entities: Set<TypePermission>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
