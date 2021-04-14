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
import com.pibity.core.entities.Subspace
import com.pibity.core.serializers.mappings.serialize

fun serialize(subspace: Subspace): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, subspace.space.organization.id)
  addProperty("subspaceName", subspace.name)
  addProperty("spaceName", subspace.space.name)
  add("typePermissions", serialize(subspace.space.spaceTypePermissions))
  add("functionPermissions", serialize(subspace.space.spaceFunctionPermissions))
}

fun serialize(entities: Set<Subspace>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
