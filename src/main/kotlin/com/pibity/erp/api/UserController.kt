/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.RoleConstants
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.serializers.serialize
import com.pibity.erp.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.security.RolesAllowed

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/user"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class UserController(val userService: UserService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createUser" to getExpectedParams("user", "createUser"),
      "updateUserGroups" to getExpectedParams("user", "updateUserGroups"),
      "updateUserRoles" to getExpectedParams("user", "updateUserRoles"),
      "getUserDetails" to getExpectedParams("user", "getUserDetails"),
      "getUserPermissions" to getExpectedParams("user", "getUserPermissions"),
      "superimposeUserPermissions" to getExpectedParams("user", "superimposeUserPermissions")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.OWNER)
  fun createUser(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(userService.createUser(jsonParams = (getJsonParams(request, expectedParams["createUser"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/groups"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.OWNER)
  fun updateUserGroups(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(userService.updateUserGroups(jsonParams = (getJsonParams(request, expectedParams["updateUserGroups"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/roles"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.OWNER)
  fun updateUserRoles(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(userService.updateUserRoles(jsonParams = (getJsonParams(request, expectedParams["updateUserRoles"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.OWNER)
  fun getUserDetails(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(userService.getUserDetails(jsonParams = (getJsonParams(request, expectedParams["getUserDetails"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/permissions"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.SUPERUSER)
  fun getUserPermissions(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(userService.getUserPermissions(jsonParams = (getJsonParams(request, expectedParams["getUserPermissions"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/superimpose"], produces = [MediaType.APPLICATION_JSON_VALUE])
  @RolesAllowed(RoleConstants.SUPERUSER)
  fun superimposeUserPermissions(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(serialize(userService.superimposeUserPermissions(jsonParams = (getJsonParams(request, expectedParams["superimposeUserPermissions"]
          ?: JsonObject())))).toString(), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
