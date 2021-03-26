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
import com.pibity.erp.entities.Group
import com.pibity.erp.serializers.mappings.serialize

fun serialize(group: Group): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, group.organization.id)
  addProperty("groupName", group.name)
  add("roles", serialize(group.groupRoles))
}

fun serialize(entities: Set<Group>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
