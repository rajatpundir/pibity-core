/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp

import org.keycloak.KeycloakSecurityContext
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import javax.servlet.http.HttpServletRequest

@SpringBootApplication
class ErpApplication

fun main(args: Array<String>) {
  runApplication<ErpApplication>(*args)
}

fun HttpServletRequest.getKeycloakSecurityContext(): KeycloakSecurityContext {
  return this.getAttribute(KeycloakSecurityContext::class.java.name) as KeycloakSecurityContext
}
