/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.utils

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.KeycloakConstants
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.commons.constants.UserConstants
import com.pibity.core.commons.constants.realmResource
import org.keycloak.admin.client.CreatedResponseUtil
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.UserRepresentation

fun createKeycloakGroup(jsonParams: JsonObject) {
  realmResource.groups().add(GroupRepresentation().apply { name = jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString }).use { response ->
    val parentGroupId: String = CreatedResponseUtil.getCreatedId(response)
    setOf(KeycloakConstants.SUBGROUP_ADMIN, KeycloakConstants.SUBGROUP_USER).forEach { subGroupName ->
      realmResource.groups().group(parentGroupId).subGroup(GroupRepresentation().apply { name = subGroupName })
    }
  }
}

fun getKeycloakId(username: String): String = realmResource.users()!!.search(username, true).single().id

fun createKeycloakUser(jsonParams: JsonObject): String {
  realmResource.users().create(UserRepresentation().apply {
    username = jsonParams.get(UserConstants.EMAIL).asString
    email = jsonParams.get(UserConstants.EMAIL).asString
    firstName = jsonParams.get(UserConstants.FIRST_NAME).asString
    lastName = jsonParams.get(UserConstants.LAST_NAME).asString
    isEnabled = true
  })
  val keycloakUserId: String = getKeycloakId(jsonParams.get(UserConstants.EMAIL).asString)
  val userResource: UserResource = realmResource.users()!!.get(keycloakUserId)
  userResource.resetPassword(CredentialRepresentation().apply {
    isTemporary = false
    type = CredentialRepresentation.PASSWORD
    value = jsonParams.get(UserConstants.PASSWORD).asString
  })
  userResource.joinGroup(realmResource.getGroupByPath(listOf(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString, KeycloakConstants.SUBGROUP_USER).joinToString(separator = "/")).id)
  if (jsonParams.has(KeycloakConstants.SUBGROUP_NAME))
    userResource.joinGroup(realmResource.getGroupByPath(listOf(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString, jsonParams.get(KeycloakConstants.SUBGROUP_NAME).asString).joinToString(separator = "/")).id)
  return keycloakUserId
}

fun joinKeycloakGroups(jsonParams: JsonObject): String {
  val keycloakUserId: String = jsonParams.get(KeycloakConstants.KEYCLOAK_USERNAME).asString
  val userResource: UserResource = realmResource.users()!!.get(keycloakUserId)
  val subGroups: List<String> = jsonParams.get(KeycloakConstants.SUBGROUP_NAME).asJsonArray.map { it.asString }
  for (subGroup in subGroups) {
    val subGroupId: String = realmResource.getGroupByPath(listOf(jsonParams.get(OrganizationConstants.ORGANIZATION_ID).asString, subGroup).joinToString(separator = "/")).id
    userResource.joinGroup(subGroupId)
  }
  return keycloakUserId
}
