/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.OrganizationConstants
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.constants.realmResource
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.UserRepresentation

fun createKeycloakGroup(jsonParams: JsonObject) {
  realmResource.groups().add(GroupRepresentation().apply { name = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString }).use { response ->
    val parentGroupId: String = CreatedResponseUtil.getCreatedId(response)
    setOf(RoleConstants.ADMIN, RoleConstants.USER).forEach { subGroupName ->
      realmResource.groups().group(parentGroupId).subGroup(GroupRepresentation().apply { name = subGroupName })
    }
  }
}

fun getKeycloakId(username: String): String = realmResource.users()!!.search(username, true).single().id

fun createKeycloakUser(jsonParams: JsonObject): String {
  realmResource.users().create(UserRepresentation().apply {
    username = jsonParams.get("email").asString
    email = jsonParams.get("email").asString
    firstName = jsonParams.get("firstName").asString
    lastName = jsonParams.get("lastName").asString
    isEnabled = true
  })
  val keycloakUserId: String = getKeycloakId(jsonParams.get("email").asString)
  val userResource: UserResource = realmResource.users()!!.get(keycloakUserId)
  userResource.resetPassword(CredentialRepresentation().apply {
    isTemporary = false
    type = CredentialRepresentation.PASSWORD
    value = jsonParams.get("password").asString
  })
  userResource.joinGroup(realmResource.getGroupByPath(listOf(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString, if (jsonParams.has("subGroupName")) jsonParams.get("subGroupName").asString else "USER").joinToString(separator = "/")).id)
  return keycloakUserId
}

fun joinKeycloakGroups(jsonParams: JsonObject): String {
  val keycloakUserId: String = jsonParams.get("keycloakUserId").asString
  val userResource: UserResource = realmResource.users()!!.get(keycloakUserId)
  val subGroups: List<String> = jsonParams.get("subGroups").asJsonArray.map { it.asString }
  for (subGroup in subGroups) {
    val subGroupId: String = realmResource.getGroupByPath(listOf(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString, subGroup).joinToString(separator = "/")).id
    userResource.joinGroup(subGroupId)
  }
  return keycloakUserId
}
