/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.GLOBAL_TYPE
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type

fun validateKeyPermissions(jsonParams: JsonObject, type: Type): JsonObject {
  val expectedKeyPermissions = JsonObject()
  for (key in type.keys) {
    if (jsonParams.has(key.name)) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          val accessLevel: Int = try {
            jsonParams.get(key.name).asInt
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
          }
          if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
            throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
          expectedKeyPermissions.addProperty(key.name, accessLevel)
        }
        TypeConstants.FORMULA -> {
          val accessLevel: Int = try {
            jsonParams.get(key.name).asInt
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
          }
          if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.READ_ACCESS)
            throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
          expectedKeyPermissions.addProperty(key.name, accessLevel)
        }
        TypeConstants.LIST -> {
          if (key.list!!.type.superTypeName == GLOBAL_TYPE) {
            val accessLevel: Int = try {
              jsonParams.get(key.name).asInt
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            expectedKeyPermissions.addProperty(key.name, accessLevel)
          } else {
            if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.list!!.type.superTypeName)
                || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.list!!.type.superTypeName)) {
              if (jsonParams.get(key.name).isJsonObject) {
                val creatable: Boolean = if (jsonParams.get(key.name).asJsonObject.has("creatable")) {
                  try {
                    jsonParams.get(key.name).asJsonObject.get("creatable").asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.name}: {creatable: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.name}: {creatable: 'Field is missing in request body'}}}")
                val deletable: Boolean = if (jsonParams.get(key.name).asJsonObject.has("creatable")) {
                  try {
                    jsonParams.get(key.name).asJsonObject.get("deletable").asBoolean
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.name}: {deletable: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.name}: {deletable: 'Field is missing in request body'}}}")
                val keyPermission: JsonObject = if (jsonParams.get(key.name).asJsonObject.has("permissions")) {
                  try {
                    jsonParams.get(key.name).asJsonObject.get("permissions").asJsonObject
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.name}: {permissions: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.name}: {permissions: 'Field is missing in request body'}}}")
                try {
                  expectedKeyPermissions.add(key.name, JsonObject().apply {
                    addProperty("creatable", creatable)
                    addProperty("deletable", deletable)
                    add("permissions", validateKeyPermissions(jsonParams = keyPermission, type = key.list!!.type))
                  })
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${key.name}: ${exception.message}}")
                }
              } else throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            } else {
              val accessLevel: Int = try {
                jsonParams.get(key.name).asInt
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              }
              if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              expectedKeyPermissions.addProperty(key.name, accessLevel)
            }
          }
        }
        else -> {
          if (key.type.superTypeName == GLOBAL_TYPE) {
            val accessLevel: Int = try {
              jsonParams.get(key.name).asInt
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            }
            if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
              throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            expectedKeyPermissions.addProperty(key.name, accessLevel)
          } else {
            if ((key.parentType.superTypeName == GLOBAL_TYPE && key.parentType.name == key.type.superTypeName)
                || (key.parentType.superTypeName != GLOBAL_TYPE && key.parentType.superTypeName == key.type.superTypeName)) {
              if (jsonParams.get(key.name).isJsonObject) {
                val keyPermission: JsonObject = if (jsonParams.get(key.name).asJsonObject.has("permissions")) {
                  try {
                    jsonParams.get(key.name).asJsonObject.get("permissions").asJsonObject
                  } catch (exception: Exception) {
                    throw CustomJsonException("{permissions: {${key.name}: {permissions: 'Unexpected value for parameter'}}}")
                  }
                } else throw CustomJsonException("{permissions: {${key.name}: {permissions: 'Field is missing in request body'}}}")
                try {
                  expectedKeyPermissions.add(key.name, JsonObject().apply {
                    add("permissions", validateKeyPermissions(jsonParams = keyPermission, type = key.type))
                  })
                } catch (exception: CustomJsonException) {
                  throw CustomJsonException("{${key.name}: ${exception.message}}")
                }
              } else throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
            } else {
              val accessLevel: Int = try {
                jsonParams.get(key.name).asInt
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              }
              if (accessLevel < PermissionConstants.NO_ACCESS || accessLevel > PermissionConstants.WRITE_ACCESS)
                throw CustomJsonException("{${key.name}: 'Unexpected value for parameter'}")
              expectedKeyPermissions.addProperty(key.name, accessLevel)
            }
          }
        }
      }
    } else throw CustomJsonException("{${key.name}: 'Field is missing in request body'}")
  }
  return expectedKeyPermissions
}
