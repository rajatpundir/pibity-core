/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.api

import com.google.gson.JsonObject
import com.pibity.erp.commons.*
import com.pibity.erp.commons.logger.Logger
import com.pibity.erp.services.TypeService
import com.pibity.erp.services.VariableService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/type"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class TypeController(val typeService: TypeService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createType" to getExpectedParams("type", "createType"),
      "getTypeDetails" to getExpectedParams("type", "getTypeDetails"),
      "addCategory" to getExpectedParams("type", "addCategory"),
      "removeCategory" to getExpectedParams("type", "removeCategory"),
      "listCategories" to getExpectedParams("type", "listCategories"),
      "listVariables" to getExpectedParams("type", "listVariables")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun createType(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(typeService.createType(jsonParams = (getJsonParams(request, expectedParams["createType"]
          ?: JsonObject())))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/details"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getTypeDetails(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(typeService.getTypeDetails(jsonParams = getJsonParams(request, expectedParams["getTypeDetails"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/category/add"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun addCategory(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(typeService.addCategory(jsonParams = getJsonParams(request, expectedParams["addCategory"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/category/remove"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun removeCategory(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(typeService.removeCategory(jsonParams = getJsonParams(request, expectedParams["removeCategory"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/categories"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun listCategories(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(typeService.listCategories(jsonParams = getJsonParams(request, expectedParams["listCategories"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/variables"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun listVariables(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(typeService.listVariables(jsonParams = getJsonParams(request, expectedParams["listVariables"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
