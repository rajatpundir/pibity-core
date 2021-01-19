/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.utils.getCircuitOutputType
import com.pibity.erp.entities.circuit.Circuit

fun serialize(circuit: Circuit): JsonObject {
  val json = JsonObject()
  json.addProperty("orgId", circuit.organization.id)
  json.addProperty("circuitName", circuit.name)
  json.add("inputs", JsonObject().apply {
    for (circuitInput in circuit.inputs)
      add(circuitInput.name, JsonObject().apply {
        addProperty(KeyConstants.KEY_TYPE, circuitInput.type.name)
        when (circuitInput.type.name) {
          TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultStringValue!!)
          TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultLongValue!!)
          TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultDoubleValue!!)
          TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultBooleanValue!!)
          TypeConstants.FORMULA, TypeConstants.LIST -> {
          }
          else -> if (circuitInput.referencedVariable != null)
            addProperty(KeyConstants.DEFAULT, circuitInput.referencedVariable!!.name)
        }
      })
  })
  json.add("computations", JsonObject().apply {
    for (computation in circuit.computations)
      add(computation.name, JsonObject().apply {
        addProperty(ORDER, computation.order)
        addProperty(LEVEL, computation.level)
        if (computation.connectedToFunction) {
          addProperty(KeyConstants.KEY_TYPE, "Function")
          addProperty(EXECUTE, computation.function!!.name)
          add(CONNECT, JsonObject().apply {
            for (connection in computation.connections)
              add(connection.functionInput!!.name, JsonArray().apply {
                if (connection.connectedToComputation) {
                  add(COMPUTATION)
                  add(connection.connectedCircuitComputation!!.name)
                  add(connection.connectedCircuitComputationFunctionOutput!!.name)
                } else {
                  add(INPUT)
                  add(connection.connectedCircuitInput!!.name)
                }
              })
          })
        } else {
          addProperty(KeyConstants.KEY_TYPE, "Circuit")
          addProperty(EXECUTE, computation.circuit!!.name)
          add(CONNECT, JsonObject().apply {
            for (connection in computation.connections)
              add(connection.circuitInput!!.name, JsonArray().apply {
                if (connection.connectedToComputation) {
                  add(COMPUTATION)
                  add(connection.connectedCircuitComputation!!.name)
                  add(connection.connectedCircuitComputationCircuitOutput!!.name)
                } else {
                  add(INPUT)
                  add(connection.connectedCircuitInput!!.name)
                }
              })
          })
        }
      })
  })
  json.add("outputs", JsonObject().apply {
    for (circuitOutput in circuit.outputs)
      add(circuitOutput.name, JsonObject().apply {
        addProperty(KeyConstants.KEY_TYPE, getCircuitOutputType(circuitOutput).name)
        add(CONNECT, JsonArray().apply {
          add(circuitOutput.connectedCircuitComputation.name)
          if (circuitOutput.connectedCircuitComputation.connectedToFunction)
            add(circuitOutput.connectedCircuitComputationFunctionOutput!!.name)
          else
            add(circuitOutput.connectedCircuitComputationCircuitOutput!!.name)
        })
      })
  })
  return json
}

fun serialize(entities: Set<Circuit>): JsonArray {
  val json = JsonArray()
  for (entity in entities)
    json.add(serialize(entity))
  return json
}
