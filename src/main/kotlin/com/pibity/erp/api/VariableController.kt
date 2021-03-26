/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.KeycloakConstants
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.serializers.serialize
import com.pibity.erp.services.QueryService
import com.pibity.erp.services.VariableService
import com.pibity.erp.commons.utils.getExpectedParams
import com.pibity.erp.commons.utils.getJsonParams
import com.pibity.erp.commons.utils.validateOrganizationClaim
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/variables"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class VariableController(val variableService: VariableService, val queryService: QueryService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "mutateVariables" to getExpectedParams("variable", "mutateVariables"),
      "queryVariables" to getExpectedParams("variable", "queryVariables")
  )

  @PostMapping(path = ["/mutate"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun mutateVariable(@RequestParam("files") files: List<MultipartFile>, @RequestParam("request") request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["mutateVariables"]
          ?: JsonObject()).apply { addProperty("username", token.subject) }
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = RoleConstants.USER)
      ResponseEntity(variableService.executeQueue(jsonParams = jsonParams, files = files).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/query"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun queryVariables(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["queryVariables"]
          ?: JsonObject()).apply { addProperty("username", token.subject) }
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = RoleConstants.USER)
      ResponseEntity(serialize(queryService.queryVariables(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/query/public"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun queryPublicVariables(@RequestBody request: String): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["queryVariables"] ?: JsonObject())
      ResponseEntity(serialize(queryService.queryPublicVariables(jsonParams = jsonParams)).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
