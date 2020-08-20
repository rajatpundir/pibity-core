/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.getExpectedParams
import com.pibity.erp.commons.getJsonParams
import com.pibity.erp.commons.gson
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/user"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class UserController(val userService: UserService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createUser" to getExpectedParams("user", "createUser"),
      "updateUserGroups" to getExpectedParams("user", "updateUserGroups"),
      "updateUserRoles" to getExpectedParams("user", "updateUserRoles"),
      "getUserDetails" to getExpectedParams("user", "getUserDetails")
      )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun createUser(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(userService.createUser(jsonParams = (getJsonParams(request, expectedParams["createUser"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/groups"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun updateUserGroups(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(userService.updateUserGroups(jsonParams = (getJsonParams(request, expectedParams["updateUserGroups"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update/roles"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun updateUserRoles(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(userService.updateUserRoles(jsonParams = (getJsonParams(request, expectedParams["updateUserRoles"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getUserDetails(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(userService.getUserDetails(jsonParams = (getJsonParams(request, expectedParams["getUserDetails"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
