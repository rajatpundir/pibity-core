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
import com.pibity.erp.commons.constants.OrganizationConstants
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.entities.permission.TypePermission

fun serialize(typePermission: TypePermission): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, typePermission.type.organization.id)
  addProperty(OrganizationConstants.TYPE_NAME, typePermission.type.name)
  addProperty("permissionName", typePermission.name)
  addProperty("creatable", typePermission.creatable)
  addProperty("deletable", typePermission.deletable)
  add("permissions", typePermission.keyPermissions.fold(JsonObject()) { acc, keyPermission ->
    acc.apply {
      addProperty(keyPermission.key.name, when (keyPermission.accessLevel) {
        PermissionConstants.READ_ACCESS -> "READ"
        PermissionConstants.WRITE_ACCESS -> "WRITE"
        else -> "NONE"
      })
    }
  })
}

fun serialize(entities: Set<TypePermission>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
