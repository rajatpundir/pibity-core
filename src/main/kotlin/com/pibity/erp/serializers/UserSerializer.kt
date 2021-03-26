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
import com.pibity.erp.entities.User
import com.pibity.erp.serializers.mappings.serialize

fun serialize(user: User): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, user.organization.id)
  addProperty(OrganizationConstants.USERNAME, user.username)
  addProperty("active", user.active)
  addProperty("email", user.email)
  addProperty("firstName", user.firstName)
  addProperty("lastName", user.lastName)
  add("details", serialize(user.details!!))
  add("groups", serialize(user.userGroups))
  add("roles", serialize(user.userRoles))
}

fun serialize(entities: Set<User>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
