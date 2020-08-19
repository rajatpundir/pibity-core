/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type

fun validateKeyPermissions(jsonParams: JsonObject, type: Type): JsonObject {
  val keyPermissions: JsonObject = try {
    jsonParams.get("permissions").asJsonObject
  } catch (exception: Exception) {
    throw CustomJsonException("{permissions: 'Unexpected value for parameter'}")
  }
  val expectedKeyPermissions = JsonObject()
  for (key in type.keys) {
    if (keyPermissions.has(key.id.name)) {
      when (key.type.id.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN -> {
          val accessLevel: Int = try {
            keyPermissions.get(key.id.name).asInt
          } catch (exception: Exception) {
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          }
          if (accessLevel < 0 || accessLevel > 2)
            throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
          expectedKeyPermissions.addProperty(key.id.name, accessLevel)
        }
        TypeConstants.FORMULA -> {
        }
        TypeConstants.LIST -> {
          println("LIST")
          if (key.list!!.type.id.superTypeName == "Any") {
            println("CHECK -- GLOBAL -- ${key.id.name}")
            val accessLevel: Int = try {
              keyPermissions.get(key.id.name).asInt
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
            if (accessLevel < 0 || accessLevel > 2)
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            expectedKeyPermissions.addProperty(key.id.name, accessLevel)
          } else {
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.list!!.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.list!!.type.id.superTypeName)) {
              println("CHECK -- LOCAL -- ${key.id.name}")
              val keyPermission: JsonObject = try {
                keyPermissions.get(key.id.name).asJsonObject
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
              try {
                expectedKeyPermissions.add(key.id.name, validateKeyPermissions(jsonParams = keyPermission, type = key.list!!.type))
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: ${exception.message}}")
              }
            } else {
              println("CHECK -- EXTERNAL -- ${key.id.name}")
              val accessLevel: Int = try {
                keyPermissions.get(key.id.name).asInt
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
              if (accessLevel < 0 || accessLevel > 2)
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              expectedKeyPermissions.addProperty(key.id.name, accessLevel)
            }
          }
        }
        else -> {
          println("VARIABLE")
          if (key.type.id.superTypeName == "Any") {
            println("CHECK -- GLOBAL VAR -- ${key.id.name}")
            val accessLevel: Int = try {
              keyPermissions.get(key.id.name).asInt
            } catch (exception: Exception) {
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            }
            if (accessLevel < 0 || accessLevel > 2)
              throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
            expectedKeyPermissions.addProperty(key.id.name, accessLevel)
          } else {
            println("#########")
            println(key.id.parentType.id.superTypeName)
            println(key.id.parentType.id.name)
            println(key.type.id.superTypeName)
            println("#########")
            if ((key.id.parentType.id.superTypeName == "Any" && key.id.parentType.id.name == key.type.id.superTypeName)
                || (key.id.parentType.id.superTypeName != "Any" && key.id.parentType.id.superTypeName == key.type.id.superTypeName)) {
              println("CHECK -- LOCAL VAR -- ${key.id.name}")
              val keyPermission: JsonObject = try {
                keyPermissions.get(key.id.name).asJsonObject
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
              try {
                expectedKeyPermissions.add(key.id.name, validateKeyPermissions(jsonParams = keyPermission, type = key.type))
              } catch (exception: CustomJsonException) {
                throw CustomJsonException("{${key.id.name}: ${exception.message}}")
              }
            } else {
              println("CHECK -- EXTERNAL VAR -- ${key.id.name}")
              val accessLevel: Int = try {
                keyPermissions.get(key.id.name).asInt
              } catch (exception: Exception) {
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              }
              if (accessLevel < 0 || accessLevel > 2)
                throw CustomJsonException("{${key.id.name}: 'Unexpected value for parameter'}")
              expectedKeyPermissions.addProperty(key.id.name, accessLevel)
            }
          }
        }
      }
    } else throw CustomJsonException("{${key.id.name}: 'Field is missing in request body'}")
  }
  return JsonObject().apply { add("permissions", expectedKeyPermissions)}
}
