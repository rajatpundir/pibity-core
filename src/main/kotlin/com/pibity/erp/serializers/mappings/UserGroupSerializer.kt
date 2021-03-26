/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers.mappings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.entities.mappings.UserGroup
import com.pibity.erp.serializers.serialize

fun serialize(userGroup: UserGroup): JsonObject = serialize(userGroup.id.group)

fun serialize(entities: Set<UserGroup>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
