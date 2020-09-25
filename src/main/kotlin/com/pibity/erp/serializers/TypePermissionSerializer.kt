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
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.entities.TypePermission

fun serialize(typePermission: TypePermission): JsonObject {
  val json = JsonObject()
  if (typePermission.id.type.id.superTypeName == "Any") {
    json.addProperty("organization", typePermission.id.type.id.organization.id)
    json.addProperty("typeName", typePermission.id.type.id.name)
    json.addProperty("permissionName", typePermission.id.name)
    json.addProperty("creatable", typePermission.creatable)
    json.addProperty("deletable", typePermission.deletable)
  }

  val jsonKeyPermissions = JsonObject()
  for (keyPermission in typePermission.keyPermissions) {
    when (keyPermission.id.key.type.id.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.FORMULA -> {
        jsonKeyPermissions.addProperty(
            keyPermission.id.key.id.name,
            when (keyPermission.accessLevel) {
              PermissionConstants.READ_ACCESS -> "READ"
              PermissionConstants.WRITE_ACCESS -> "WRITE"
              else -> "NONE"
            })
      }
      TypeConstants.LIST -> {
        if (keyPermission.id.key.list!!.type.id.superTypeName == "Any")
          jsonKeyPermissions.addProperty(
              keyPermission.id.key.id.name,
              when (keyPermission.accessLevel) {
                PermissionConstants.READ_ACCESS -> "READ"
                PermissionConstants.WRITE_ACCESS -> "WRITE"
                else -> "NONE"
              })
        else {
          if ((keyPermission.id.key.id.parentType.id.superTypeName == "Any" && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.list!!.type.id.superTypeName)
              || (keyPermission.id.key.id.parentType.id.superTypeName != "Any" && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.list!!.type.id.superTypeName))
            jsonKeyPermissions.add(keyPermission.id.key.id.name, serialize(keyPermission.referencedTypePermission!!))
          else
            jsonKeyPermissions.addProperty(
                keyPermission.id.key.id.name,
                when (keyPermission.accessLevel) {
                  PermissionConstants.READ_ACCESS -> "READ"
                  PermissionConstants.WRITE_ACCESS -> "WRITE"
                  else -> "NONE"
                })
        }
      }
      else -> {
        if (keyPermission.id.key.type.id.superTypeName == "Any")
          jsonKeyPermissions.addProperty(
              keyPermission.id.key.id.name,
              when (keyPermission.accessLevel) {
                PermissionConstants.READ_ACCESS -> "READ"
                PermissionConstants.WRITE_ACCESS -> "WRITE"
                else -> "NONE"
              })
        else {
          if ((keyPermission.id.key.id.parentType.id.superTypeName == "Any" && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.type.id.superTypeName)
              || (keyPermission.id.key.id.parentType.id.superTypeName != "Any" && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.type.id.superTypeName))
            jsonKeyPermissions.add(keyPermission.id.key.id.name, serialize(keyPermission.referencedTypePermission!!))
          else
            jsonKeyPermissions.addProperty(
                keyPermission.id.key.id.name,
                when (keyPermission.accessLevel) {
                  PermissionConstants.READ_ACCESS -> "READ"
                  PermissionConstants.WRITE_ACCESS -> "WRITE"
                  else -> "NONE"
                })
        }
      }
    }
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
