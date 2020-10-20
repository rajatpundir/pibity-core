/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeycloakConstants
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.commons.utils.getExpectedParams
import com.pibity.erp.commons.utils.getJsonParams
import com.pibity.erp.services.FunctionService
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/function"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class FunctionController(val functionService: FunctionService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createFunction" to getExpectedParams("function", "createFunction"),
      "executeFunction" to getExpectedParams("function", "executeFunction")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_SUPERUSER)
  fun createFunction(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
//    val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
//    val jsonParams: JsonObject = getJsonParams(request, expectedParams["createFunction"]
//        ?: JsonObject()).apply { addProperty("username", token.subject) }
//    return ResponseEntity(functionService.createFunction(jsonParams = jsonParams).toString(), HttpStatus.OK)
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createFunction"]
          ?: JsonObject()).apply { addProperty("username", token.subject) }
      ResponseEntity(functionService.createFunction(jsonParams = jsonParams).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/execute"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun executeFunction(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["executeFunction"]
          ?: JsonObject()).apply { addProperty("username", token.subject) }
      ResponseEntity(functionService.executeFunction(jsonParams = jsonParams).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}