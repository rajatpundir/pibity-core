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
import com.pibity.core.entities.Space
import com.pibity.core.serializers.mappings.serialize

fun serialize(space: Space): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, space.organization.id)
  addProperty("spaceName", space.name)
  add("typePermissions", serialize(space.spaceTypePermissions))
  add("functionPermissions", serialize(space.spaceFunctionPermissions))
}

fun serialize(entities: Set<Space>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
