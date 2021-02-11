/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.PermissionConstants
import com.pibity.erp.commons.constants.TypeConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type

fun validateKeyPermissions(jsonParams: JsonObject, type: Type): JsonObject {
  val expectedKeyPermissions = JsonObject()
  for (key in type.keys) {
    if (jsonParams.has(key.name)) {
      when (key.type.name) {
        TypeConstants.TEXT, TypeConstants.NUMBER, TypeConstants.DECIMAL, TypeConstants.BOOLEAN, TypeConstants.DATE, TypeConstants.TIME, TypeConstants.TIMESTAMP, TypeConstants.BLOB -> {
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
             else -> {
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
      } else throw CustomJsonException("{${key.name}: 'Field is missing in request body'}")
  }
  return expectedKeyPermissions
}
