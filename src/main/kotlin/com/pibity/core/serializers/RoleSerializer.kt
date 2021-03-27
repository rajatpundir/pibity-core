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
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.entities.Role
import com.pibity.core.serializers.mappings.serialize

fun serialize(role: Role): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, role.organization.id)
  addProperty("roleName", role.name)
  add("typePermissions", serialize(role.roleTypePermissions))
  add("functionPermissions", serialize(role.roleFunctionPermissions))
}

fun serialize(entities: Set<Role>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
