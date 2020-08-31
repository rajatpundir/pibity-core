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

fun serialize(src: TypePermission): JsonObject {
  val json = JsonObject()
  if (src.id.type.id.superTypeName == "Any") {
    json.addProperty("organization", src.id.type.id.organization.id)
    json.addProperty("typeName", src.id.type.id.name)
    json.addProperty("permissionName", src.id.name)
    json.addProperty("creatable", src.creatable)
    json.addProperty("deletable", src.deletable)
  }
  json.addProperty(
      "maxAccessLevel",
      when (src.maxAccessLevel) {
        PermissionConstants.READ_ACCESS -> "READ"
        PermissionConstants.WRITE_ACCESS -> "WRITE"
        else -> "NONE"
      })
  json.addProperty(
      "minAccessLevel",
      when (src.minAccessLevel) {
        PermissionConstants.READ_ACCESS -> "READ"
        PermissionConstants.WRITE_ACCESS -> "WRITE"
        else -> "NONE"
      })
  val jsonKeyPermissions = JsonObject()
  for (keyPermission in src.keyPermissions) {
    when (keyPermission.id.key.type.id.name) {
      TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
        jsonKeyPermissions.addProperty(
            keyPermission.id.key.id.name,
            when (keyPermission.accessLevel) {
              PermissionConstants.READ_ACCESS -> "READ"
              PermissionConstants.WRITE_ACCESS -> "WRITE"
              else -> "NONE"
            })
      }
      TypeConstants.FORMULA -> {
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

fun serialize(src: Set<TypePermission>): JsonArray {
  val json = JsonArray()
  for (entity in src)
    json.add(serialize(entity))
  return json
}
