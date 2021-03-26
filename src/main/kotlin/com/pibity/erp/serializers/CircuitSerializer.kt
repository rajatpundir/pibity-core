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
import com.pibity.erp.commons.utils.getCircuitOutput
import com.pibity.erp.entities.circuit.Circuit

fun serialize(circuit: Circuit): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, circuit.organization.id)
  addProperty(CircuitConstants.CIRCUIT_NAME, circuit.name)
  circuit.inputs.fold(JsonObject()) { acc, circuitInput ->
    acc.apply {
      add(circuitInput.name, JsonObject().apply {
        addProperty(KeyConstants.KEY_TYPE, circuitInput.type.name)
        when (circuitInput.type.name) {
          TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultStringValue!!)
          TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultLongValue!!)
          TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultDecimalValue!!)
          TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultBooleanValue!!)
          TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultDateValue!!.time)
          TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultTimestampValue!!.time)
          TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultTimeValue!!.time)
          TypeConstants.BLOB -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultBlobValue!!.toString())
          TypeConstants.FORMULA -> {
          }
          else -> if (circuitInput.referencedVariable != null)
            addProperty(KeyConstants.DEFAULT, circuitInput.referencedVariable!!.name)
        }
      })
    }
  }
  add(CircuitConstants.INPUTS, JsonObject())
  add(CircuitConstants.COMPUTATIONS, JsonObject().apply {
    for (computation in circuit.computations)
      add(computation.name, JsonObject().apply {
        addProperty(CircuitConstants.ORDER, computation.order)
        addProperty(CircuitConstants.LEVEL, computation.level)
        if (computation.function != null) {
          addProperty(KeyConstants.KEY_TYPE, "Function")
          addProperty(CircuitConstants.EXECUTE, computation.function!!.name)
          add(CircuitConstants.CONNECT, JsonObject().apply {
            for (connection in computation.connections)
              add(connection.functionInput!!.name, JsonArray().apply {
                if (connection.connectedCircuitComputation != null) {
                  add(CircuitConstants.COMPUTATION)
                  add(connection.connectedCircuitComputation!!.name)
                  add(connection.connectedCircuitComputationFunctionOutput!!.name)
                } else {
                  add(CircuitConstants.INPUT)
                  add(connection.connectedCircuitInput!!.name)
                }
              })
          })
        } else {
          addProperty(KeyConstants.KEY_TYPE, "Circuit")
          addProperty(CircuitConstants.EXECUTE, computation.circuit!!.name)
          add(CircuitConstants.CONNECT, JsonObject().apply {
            for (connection in computation.connections)
              add(connection.circuitInput!!.name, JsonArray().apply {
                if (connection.connectedCircuitComputation != null) {
                  add(CircuitConstants.COMPUTATION)
                  add(connection.connectedCircuitComputation!!.name)
                  add(connection.connectedCircuitComputationCircuitOutput!!.name)
                } else {
                  add(CircuitConstants.INPUT)
                  add(connection.connectedCircuitInput!!.name)
                }
              })
          })
        }
      })
  })
  add(CircuitConstants.OUTPUTS, JsonObject().apply {
    for (circuitOutput in circuit.outputs)
      add(circuitOutput.name, JsonObject().apply {
        addProperty(KeyConstants.KEY_TYPE, getCircuitOutput(circuitOutput).name)
        add(CircuitConstants.CONNECT, JsonArray().apply {
          add(circuitOutput.connectedCircuitComputation.name)
          if (circuitOutput.connectedCircuitComputation.function != null)
            add(circuitOutput.connectedCircuitComputationFunctionOutput!!.name)
          else
            add(circuitOutput.connectedCircuitComputationCircuitOutput!!.name)
        })
      })
  })
}

fun serialize(entities: Set<Circuit>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
