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
import com.pibity.erp.services.CategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping(path = ["/api/category"], consumes = [MediaType.APPLICATION_JSON_VALUE])
class CategoryController(val categoryService: CategoryService) {

  private val logger by Logger()

  private val expectedParams: Map<String, JsonObject> = mapOf(
      "createCategory" to getExpectedParams("category", "createCategory"),
      "updateCategory" to getExpectedParams("category", "updateCategory"),
      "removeCategory" to getExpectedParams("category", "removeCategory"),
      "listTypes" to getExpectedParams("category", "listTypes")
  )

  @PostMapping(path = ["/create"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun createCategory(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(categoryService.createCategory(jsonParams = getJsonParams(request, expectedParams["createCategory"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/update"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun updateCategory(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(categoryService.updateCategory(jsonParams = getJsonParams(request, expectedParams["updateCategory"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/remove"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun removeCategory(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(categoryService.removeCategory(jsonParams = getJsonParams(request, expectedParams["removeCategory"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }

  @PostMapping(path = ["/types"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun listTypes(@RequestBody request: String): ResponseEntity<String> {
    return try {
      ResponseEntity(gson.toJson(categoryService.listTypes(jsonParams = getJsonParams(request, expectedParams["listTypes"]
          ?: JsonObject()))), HttpStatus.OK)
    } catch (exception: Exception) {
      val message: String = exception.message ?: "Unable to process your request"
      logger.info("Exception caused via request: $request with message: $message")
      ResponseEntity(message, HttpStatus.BAD_REQUEST)
    }
  }
}
