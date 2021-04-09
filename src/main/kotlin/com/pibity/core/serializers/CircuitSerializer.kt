/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.serializers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.core.commons.constants.*
import com.pibity.core.commons.CustomJsonException
import com.pibity.core.entities.accumulator.TypeAccumulator
import com.pibity.core.entities.circuit.Circuit
import java.util.*

fun serialize(circuit: Circuit): JsonObject = JsonObject().apply {
  addProperty(OrganizationConstants.ORGANIZATION_ID, circuit.organization.id)
  addProperty(CircuitConstants.CIRCUIT_NAME, circuit.name)
  add(CircuitConstants.INPUTS, circuit.inputs.fold(JsonObject()) { acc, circuitInput ->
    acc.apply {
      add(circuitInput.name, JsonObject().apply {
        if (circuitInput.type != null) {
          addProperty(KeyConstants.KEY_TYPE, circuitInput.type.name)
          when (circuitInput.type.name) {
            TypeConstants.TEXT -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultStringValue!!)
            TypeConstants.NUMBER -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultLongValue!!)
            TypeConstants.DECIMAL -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultDecimalValue!!)
            TypeConstants.BOOLEAN -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultBooleanValue!!)
            TypeConstants.DATE -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultDateValue!!.time)
            TypeConstants.TIMESTAMP -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultTimestampValue!!.time)
            TypeConstants.TIME -> addProperty(KeyConstants.DEFAULT, circuitInput.defaultTimeValue!!.time)
            TypeConstants.BLOB -> addProperty(KeyConstants.DEFAULT, Base64.getEncoder().encodeToString(circuitInput.defaultBlobValue!!.getBytes(1, circuitInput.defaultBlobValue!!.length().toInt())))
            TypeConstants.FORMULA -> throw CustomJsonException("{}")
            else -> if (circuitInput.referencedVariable != null)
              addProperty(KeyConstants.DEFAULT, circuitInput.referencedVariable!!.name)
          }
        } else add(KeyConstants.KEY_TYPE, JsonArray())
      })
    }
  })
  add(CircuitConstants.COMPUTATIONS, circuit.computations.fold(JsonObject()) { acc, computation ->
    acc.apply {
      add(computation.name, JsonObject().apply {
        addProperty(CircuitConstants.ORDER, computation.order)
        addProperty(CircuitConstants.LEVEL, computation.level)
        if (computation.function != null) {
          addProperty(KeyConstants.KEY_TYPE, CircuitConstants.FUNCTION)
          addProperty(CircuitConstants.EXECUTE, computation.function.name)
          add(CircuitConstants.CONNECT, computation.connections.fold(JsonObject()) { acc1, connection ->
            acc1.apply {
              add(connection.functionInput!!.name, JsonArray().apply {
                if (connection.connectedCircuitInput != null) {
                  add(CircuitConstants.INPUT)
                  add(connection.connectedCircuitInput.name)
                } else if(connection.connectedCircuitComputation != null) {
                  add(CircuitConstants.COMPUTATION)
                  add(connection.connectedCircuitComputation.name)
                  if (connection.connectedCircuitComputationFunctionOutput != null)
                    add(connection.connectedCircuitComputationFunctionOutput.name)
                  else
                    add(connection.connectedCircuitComputationCircuitOutput!!.name)
                } else {
                  val typeAccumulator: TypeAccumulator = connection.connectedCircuitComputationTypeAccumulator!!
                  add(typeAccumulator.typeUniqueness.type.name)
                  add(typeAccumulator.typeUniqueness.name)
                  add(typeAccumulator.name)
                  add(connection.connectedCircuitComputationAccumulatorKeys.fold(JsonObject()) { acc2, connectionAccumulatorKey ->
                    acc2.apply {
                      addProperty(connectionAccumulatorKey.key.name, connectionAccumulatorKey.circuitInput.name)
                    }
                  })
                }
              })
            }
          })
        } else if(computation.circuit != null) {
          addProperty(KeyConstants.KEY_TYPE, CircuitConstants.CIRCUIT)
          addProperty(CircuitConstants.EXECUTE, computation.circuit.name)
          add(CircuitConstants.CONNECT, computation.connections.fold(JsonObject()) { acc1, connection ->
            acc1.apply {
              add(connection.circuitInput!!.name, JsonArray().apply {
                if (connection.connectedCircuitInput != null) {
                  add(CircuitConstants.INPUT)
                  add(connection.connectedCircuitInput.name)
                } else if(connection.connectedCircuitComputation != null) {
                  add(CircuitConstants.COMPUTATION)
                  add(connection.connectedCircuitComputation.name)
                  if (connection.connectedCircuitComputationFunctionOutput != null)
                    add(connection.connectedCircuitComputationFunctionOutput.name)
                  else
                    add(connection.connectedCircuitComputationCircuitOutput!!.name)
                } else {
                  val typeAccumulator: TypeAccumulator = connection.connectedCircuitComputationTypeAccumulator!!
                  add(typeAccumulator.typeUniqueness.type.name)
                  add(typeAccumulator.typeUniqueness.name)
                  add(typeAccumulator.name)
                  add(connection.connectedCircuitComputationAccumulatorKeys.fold(JsonObject()) { acc2, connectionAccumulatorKey ->
                    acc2.apply {
                      addProperty(connectionAccumulatorKey.key.name, connectionAccumulatorKey.circuitInput.name)
                    }
                  })
                }
              })
            }
          })
        } else {
          addProperty(KeyConstants.KEY_TYPE, CircuitConstants.MAPPER)
          addProperty(CircuitConstants.EXECUTE, computation.mapper!!.name)
          add(CircuitConstants.CONNECT, JsonObject().apply {
            add(MapperConstants.QUERY_PARAMS, computation.connections.fold(JsonObject()) { acc1, connection ->
              acc1.apply {
                add(connection.functionInput!!.name, JsonArray().apply {
                  if (connection.connectedCircuitInput != null) {
                    add(CircuitConstants.INPUT)
                    add(connection.connectedCircuitInput.name)
                  } else {
                    add(CircuitConstants.COMPUTATION)
                    add(connection.connectedCircuitComputation!!.name)
                    if (connection.connectedCircuitComputationFunctionOutput != null)
                      add(connection.connectedCircuitComputationFunctionOutput.name)
                    else
                      add(connection.connectedCircuitComputationCircuitOutput!!.name)
                  }
                })
              }
            })
            add(CircuitConstants.ARGS, JsonArray().apply {
              if (computation.connectedMapperCircuitInput != null) {
                add(CircuitConstants.INPUT)
                add(computation.connectedMapperCircuitInput!!.name)
              } else {
                add(CircuitConstants.COMPUTATION)
                add(computation.connectedMapperCircuitComputation!!.name)
                add(computation.connectedMapperCircuitComputationConnections.fold(JsonObject()) { acc1, connection ->
                  acc1.apply { addProperty(connection.functionInput.name, connection.referencedMapperFunctionOutput.name) }
                })
              }
            })
          })
        }
      })
    }
  })
  add(CircuitConstants.OUTPUTS, circuit.outputs.fold(JsonObject()) { acc, circuitOutput ->
    acc.apply {
      add(circuitOutput.name, JsonArray().apply {
        add(circuitOutput.connectedCircuitComputation.name)
        if (circuitOutput.connectedCircuitComputationFunctionOutput != null)
          add(circuitOutput.connectedCircuitComputationFunctionOutput.name)
        else if (circuitOutput.connectedCircuitComputationCircuitOutput != null)
          add(circuitOutput.connectedCircuitComputationCircuitOutput.name)
      })
    }
  })
}

fun serialize(entities: Set<Circuit>): JsonArray = entities.fold(JsonArray()) { acc, entity -> acc.apply { add(serialize(entity)) } }
