/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.api

import com.google.gson.JsonObject
import com.pibity.core.commons.constants.KeycloakConstants
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.commons.Logger
import com.pibity.core.commons.constants.OrganizationConstants
import com.pibity.core.utils.getExpectedParams
import com.pibity.core.utils.getJsonParams
import com.pibity.core.services.FunctionService
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
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
  fun createFunction(@RequestParam("files") files: List<MultipartFile>, @RequestParam("request") request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["createFunction"]
          ?: JsonObject()).apply { addProperty(OrganizationConstants.USERNAME, token.subject) }
      ResponseEntity(functionService.createFunction(jsonParams = jsonParams, files = files, defaultTimestamp = Timestamp.valueOf(
        ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime())).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/execute"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun executeFunction(@RequestParam("files") files: MutableList<MultipartFile>, @RequestParam("request") request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["executeFunction"]
          ?: JsonObject()).apply { addProperty(OrganizationConstants.USERNAME, token.subject) }
      ResponseEntity(functionService.executeFunction(jsonParams = jsonParams, files = files, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime())).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
