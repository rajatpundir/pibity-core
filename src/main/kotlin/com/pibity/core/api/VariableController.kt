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
import com.pibity.core.commons.constants.RoleConstants
import com.pibity.core.commons.exceptions.CustomJsonException
import com.pibity.core.commons.logger.Logger
import com.pibity.core.services.QueryService
import com.pibity.core.services.VariableService
import com.pibity.core.utils.getExpectedParams
import com.pibity.core.utils.getJsonParams
import com.pibity.core.utils.validateOrganizationClaim
import com.pibity.core.entities.Variable
import com.pibity.core.entities.permission.TypePermission
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.sql.Timestamp
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
      ResponseEntity(variableService.executeQueue(jsonParams = jsonParams, files = files, defaultTimestamp = Timestamp(System.currentTimeMillis())).toString(), HttpStatus.OK)
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
      val (variables: List<Variable>, typePermission: TypePermission) = queryService.queryVariables(jsonParams = jsonParams, defaultTimestamp = Timestamp(System.currentTimeMillis()))
      ResponseEntity(variableService.serialize(variables = variables.toSet(), typePermission = typePermission).toString(), HttpStatus.OK)
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
      val (variables: List<Variable>, typePermission: TypePermission) = queryService.queryPublicVariables(jsonParams = jsonParams, defaultTimestamp = Timestamp(System.currentTimeMillis()))
      ResponseEntity(variableService.serialize(variables = variables.toSet(), typePermission = typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
