/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.constants

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.resource.RealmResource

object KeycloakConstants {
  const val ROLE_SUPERUSER = "SUPERUSER"
  const val ROLE_USER = "USER"
  const val SUPERUSER_USERNAME = "superuser@pibity.com"
  const val SUPERUSER_PASSWORD = "1234"
  const val SERVER_URL = "http://localhost:8081/auth"
  const val REALM = ApplicationConstants.SCHEMA
  const val CLIENT_ID = "pibity-erp-admin"
  const val CLIENT_SECRET = "50064549-51bd-42aa-8bb4-c9e8e2f772fa"
}

val keycloak: Keycloak = Keycloak.getInstance(
    KeycloakConstants.SERVER_URL,
    KeycloakConstants.REALM,
    KeycloakConstants.SUPERUSER_USERNAME,
    KeycloakConstants.SUPERUSER_PASSWORD,
    KeycloakConstants.CLIENT_ID,
    KeycloakConstants.CLIENT_SECRET)

val realmResource: RealmResource = keycloak.realm(KeycloakConstants.REALM)
