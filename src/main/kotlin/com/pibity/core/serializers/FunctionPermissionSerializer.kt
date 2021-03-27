/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.FunctionConstants
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.entities.permission.FunctionPermission

fun serialize(functionPermission: FunctionPermission): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, functionPermission.function.organization.id)
  addProperty(FunctionConstants.FUNCTION_NAME, functionPermission.function.name)
  addProperty("permissionName", functionPermission.name)
  add("permissions", JsonObject().apply {
    functionPermission.functionInputPermissions.fold(JsonObject()) { acc, functionInputPermission ->
      acc.apply { addProperty(functionInputPermission.functionInput.name, functionInputPermission.accessLevel) }
    }
    functionPermission.functionOutputPermissions.fold(JsonObject()) { acc, functionOutputPermission ->
      acc.apply { addProperty(functionOutputPermission.functionOutput.name, functionOutputPermission.accessLevel) }
    }
  })
}

fun serialize(entities: Set<FunctionPermission>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
