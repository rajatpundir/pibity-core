/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.serializers

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.gson
import com.pibity.erp.entities.TypePermission
import java.lang.reflect.Type

class TypePermissionSerializer : JsonSerializer<TypePermission> {
  override fun serialize(src: TypePermission?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val json = JsonObject()
    if (src != null) {
      if (src.id.type.id.superTypeName == "Any") {
        json.addProperty("organization", src.id.type.id.organization.id)
        json.addProperty("typeName", src.id.type.id.name)
        json.addProperty("permissionName", src.id.name)
      }
      json.addProperty(
          "highestAccessLevel",
          when (src.highestAccessLevel) {
            1 -> "READ"
            2 -> "WRITE"
            else -> "NONE"
          })
      json.addProperty(
          "lowestAccessLevel",
          when (src.lowestAccessLevel) {
            1 -> "READ"
            2 -> "WRITE"
            else -> "NONE"
          })
      val jsonKeyPermissions = JsonObject()
      for (keyPermission in src.keyPermissions) {
        when (keyPermission.id.key.type.id.name) {
          TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
            jsonKeyPermissions.addProperty(
                keyPermission.id.key.id.name,
                when (keyPermission.accessLevel) {
                  1 -> "READ"
                  2 -> "WRITE"
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
                    1 -> "READ"
                    2 -> "WRITE"
                    else -> "NONE"
                  })
            else {
              if ((keyPermission.id.key.id.parentType.id.superTypeName == "Any" && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.list!!.type.id.superTypeName)
                  || (keyPermission.id.key.id.parentType.id.superTypeName != "Any" && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.list!!.type.id.superTypeName))
                jsonKeyPermissions.add(keyPermission.id.key.id.name, gson.fromJson(gson.toJson(keyPermission.referencedTypePermission), JsonObject::class.java))
              else
                jsonKeyPermissions.addProperty(
                    keyPermission.id.key.id.name,
                    when (keyPermission.accessLevel) {
                      1 -> "READ"
                      2 -> "WRITE"
                      else -> "NONE"
                    })
            }
          }
          else -> {
            if (keyPermission.id.key.type.id.superTypeName == "Any")
              jsonKeyPermissions.addProperty(
                  keyPermission.id.key.id.name,
                  when (keyPermission.accessLevel) {
                    1 -> "READ"
                    2 -> "WRITE"
                    else -> "NONE"
                  })
            else {
              if ((keyPermission.id.key.id.parentType.id.superTypeName == "Any" && keyPermission.id.key.id.parentType.id.name == keyPermission.id.key.type.id.superTypeName)
                  || (keyPermission.id.key.id.parentType.id.superTypeName != "Any" && keyPermission.id.key.id.parentType.id.superTypeName == keyPermission.id.key.type.id.superTypeName))
                jsonKeyPermissions.add(keyPermission.id.key.id.name, gson.fromJson(gson.toJson(keyPermission.referencedTypePermission), JsonObject::class.java))
              else
                jsonKeyPermissions.addProperty(
                    keyPermission.id.key.id.name,
                    when (keyPermission.accessLevel) {
                      1 -> "READ"
                      2 -> "WRITE"
                      else -> "NONE"
                    })
            }
          }
        }
      }
      json.add("permissions", jsonKeyPermissions)
    }
    return json
  }
}
