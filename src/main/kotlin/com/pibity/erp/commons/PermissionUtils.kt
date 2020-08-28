/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type

fun validateKeyPermissions(jsonParams: JsonObject, type: Type): JsonObject {
  println("-----------------------------")
  println(jsonParams)
  val expectedKeyPermissions = JsonObject()
  for (key in type.keys) {
    if (jsonParams.has(key.id.name)) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          val accessLevel: Int = try {
            jsonParams.get(key.id.name).asInt
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
          if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          expectedKeyPermissions.addProperty(key.id.name, accessLevel)
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.id.superTypeName == GLOBAL_TYPE) {
            val accessLevel: Int = try {
              jsonParams.get(key.id.name).asInt
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
            if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            expectedKeyPermissions.addProperty(key.id.name, accessLevel)
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              if (jsonParams.get(key.id.name).isJsonObject) {
                val creatable: Boolean = if (jsonParams.get(key.id.name).asJsonObject.has("creatable")) {
                  try {
                    jsonParams.get(key.id.name).asJsonObject.get("creatable").asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.id.name}: {creatable: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.id.name}: {creatable: 'Field is missing in request body'}}}")
                val deletable: Boolean = if (jsonParams.get(key.id.name).asJsonObject.has("creatable")) {
                  try {
                    jsonParams.get(key.id.name).asJsonObject.get("deletable").asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.id.name}: {deletable: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.id.name}: {deletable: 'Field is missing in request body'}}}")
                val keyPermission: JsonObject = if (jsonParams.get(key.id.name).asJsonObject.has("permissions")) {
                  try {
                    jsonParams.get(key.id.name).asJsonObject.get("permissions").asJsonObject
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.id.name}: {permissions: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.id.name}: {permissions: 'Field is missing in request body'}}}")
                try {
                  expectedKeyPermissions.add(key.id.name, JsonObject().apply {
                    addProperty("creatable", creatable)
                    addProperty("deletable", deletable)
                    add("permissions", validateKeyPermissions(jsonParams = keyPermission, type = key.list!!.type))
                  })
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${key.id.name}: ${exception.message}}")
                }
              } else throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            } else {
              val accessLevel: Int = try {
                jsonParams.get(key.id.name).asInt
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
              if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              expectedKeyPermissions.addProperty(key.id.name, accessLevel)
            }
          }
        }
        else -> {
          if (key.type.id.superTypeName == GLOBAL_TYPE) {
            val accessLevel: Int = try {
              jsonParams.get(key.id.name).asInt
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
            if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            expectedKeyPermissions.addProperty(key.id.name, accessLevel)
          } else {
            if ((key.id.parentType.id.superTypeName == GLOBAL_TYPE && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != GLOBAL_TYPE && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              if (jsonParams.get(key.id.name).isJsonObject) {
                val keyPermission: JsonObject = if (jsonParams.get(key.id.name).asJsonObject.has("permissions")) {
                  try {
                    jsonParams.get(key.id.name).asJsonObject.get("permissions").asJsonObject
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.id.name}: {permissions: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.id.name}: {permissions: 'Field is missing in request body'}}}")
                try {
                  expectedKeyPermissions.add(key.id.name, JsonObject().apply {
                    add("permissions", validateKeyPermissions(jsonParams = keyPermission, type = key.type))
                  })
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${key.id.name}: ${exception.message}}")
                }
              } else throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            } else {
              val accessLevel: Int = try {
                jsonParams.get(key.id.name).asInt
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
              if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              expectedKeyPermissions.addProperty(key.id.name, accessLevel)
            }
          }
        }
      }
    } else throw CustomJsonException("{${key.id.name}: 'Field is missing in request body'}")
  }
  return expectedKeyPermissions
}
