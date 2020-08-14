/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

//import org.keycloak.KeycloakSecurityContext
import com.google.gson.JsonObject
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.services.OrganizationService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@CrossOrigin
@RestController
@RequestMapping(path = ["/api/organization"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class OrganizationController(val organizationService: OrganizationService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createOrganization" to getExpectedParams("organization", "createOrganization")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun createOrganization(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(organizationService.createOrganization(jsonParams = getJsonParams(request, expectedParams["createOrganization"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

//  @PostMapping(path = ["/test"], produces = [MediaType.APPLICATION_JSON_VALUE])
//  fun test(request: HttpServletRequest) {
//    ResponseEntity(getJWTToken(request), HttpStatus.OK)
//  }
//
//  fun getJWTToken(request: HttpServletRequest): String? {
////    val keycloakPrincipal = (SecurityContextHolder.getContext().authentication as KeycloakAuthenticationToken).principal as KeycloakPrincipal<KeycloakSecurityContext>
//    val securityContext = getKeycloakSecurityContext(request)
//    if (securityContext != null) {
//      println(securityContext.realm)
//      println(securityContext.token.allowedOrigins.toString())
////      println(securityContext.token.realmAccess.roles.toString())
////      println(securityContext.token.realmAccess.isUserInRole("ADMIN"))
////      println(securityContext.token.realmAccess.addRole("ADMIN"))
//      println("---")
//      for (r in securityContext.token.resourceAccess.entries) {
//        println(r.key)
//        println(r.value.roles.toString())
//      }
//      println("---")
//      println(securityContext.token.resourceAccess.entries.toString())
//      println(securityContext.token.toString())
//    }
//    return securityContext?.token?.email ?: "Whatever"
//  }
//
//  private fun getKeycloakSecurityContext(request: HttpServletRequest): KeycloakSecurityContext? {
//    return request.getAttribute(KeycloakSecurityContext::class.java.name) as KeycloakSecurityContext
//  }

}
