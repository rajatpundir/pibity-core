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
import com.pibity.core.services.VariableQueryService
import com.pibity.core.services.VariableService
import com.pibity.core.utils.getExpectedParams
import com.pibity.core.utils.getJsonParams
import com.pibity.core.utils.validateOrganizationClaim
import com.pibity.core.entities.Variable
import com.pibity.core.entities.accumulator.VariableAccumulator
import com.pibity.core.entities.permission.TypePermission
import com.pibity.core.services.AccumulatorQueryService
import com.pibity.core.services.AccumulatorService
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken
import org.keycloak.representations.AccessToken
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/query"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class QueryController(
  val variableService: VariableService,
  val variableQueryService: VariableQueryService,
  val accumulatorService: AccumulatorService,
  val accumulatorQueryService: AccumulatorQueryService
) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
    "queryVariables" to getExpectedParams("query", "queryVariables"),
    "queryAccumulators" to getExpectedParams("query", "queryAccumulators")
  )

  @PostMapping(path = ["/variables"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun queryVariables(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["queryVariables"]
        ?: JsonObject()).apply { addProperty(OrganizationConstants.USERNAME, token.subject) }
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_USER)
      val (variables: List<Variable>, typePermission: TypePermission) = variableQueryService.queryVariables(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(
        ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(variableService.serialize(variables = variables.toSet(), typePermission = typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/variables/public"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun queryPublicVariables(@RequestBody request: String): ResponseEntity<String> {
    return try {
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["queryVariables"] ?: JsonObject())
      val (variables: List<Variable>, typePermission: TypePermission) = variableQueryService.queryPublicVariables(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(variableService.serialize(variables = variables.toSet(), typePermission = typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/accumulators"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(KeycloakConstants.ROLE_USER)
  fun queryAccumulators(@RequestBody request: String, authentication: KeycloakAuthenticationToken): ResponseEntity<String> {
    return try {
      val token: AccessToken = (authentication.details as SimpleKeycloakAccount).keycloakSecurityContext.token
      val jsonParams: JsonObject = getJsonParams(request, expectedParams["queryAccumulators"]
        ?: JsonObject()).apply { addProperty(OrganizationConstants.USERNAME, token.subject) }
      validateOrganizationClaim(authentication = authentication, jsonParams = jsonParams, subGroupName = KeycloakConstants.SUBGROUP_USER)
      val (variableAccumulators: List<VariableAccumulator>, typePermission: TypePermission) = accumulatorQueryService.queryAccumulators(jsonParams = jsonParams, defaultTimestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()))
      ResponseEntity(accumulatorService.serialize(variableAccumulators = variableAccumulators.toSet(), typePermission = typePermission).toString(), HttpStatus.OK)
    } catch (exception: CustomJsonException) {
      val message: String = exception.message
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
