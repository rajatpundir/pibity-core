/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.commons.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.pibity.erp.commons.constants.*
import com.pibity.erp.commons.exceptions.CustomJsonException
import com.pibity.erp.entities.Type
import com.pibity.erp.entities.circuit.Circuit
import com.pibity.erp.entities.circuit.CircuitInput
import com.pibity.erp.entities.circuit.CircuitOutput
import com.pibity.erp.entities.function.Function
import com.pibity.erp.entities.function.FunctionInput

fun validateCircuitName(circuitName: String): String {
  if (!keyIdentifierPattern.matcher(circuitName).matches())
    throw CustomJsonException("{circuitName: 'Circuit name $circuitName is not a valid identifier'}")
  return circuitName
}

fun validateCircuitInputs(inputs: JsonObject, globalTypes: Set<Type>): JsonObject {
  val expectedInputs = JsonObject()
  for ((inputName, inputElement) in inputs.entrySet()) {
    val expectedInput = JsonObject()
    if (!keyIdentifierPattern.matcher(inputName).matches())
      throw CustomJsonException("{inputs: {$inputName: 'Input name is not a valid identifier'}}")
    val inputType: Type = try {
      globalTypes.single { it.name == inputElement.asJsonObject.get(KeyConstants.KEY_TYPE).asString }
    } catch (exception: Exception) {
      throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
    }
    expectedInput.addProperty(KeyConstants.KEY_TYPE, inputType.name)
    if (inputElement.asJsonObject.has(KeyConstants.DEFAULT)) {
      when (inputType.name) {
        TypeConstants.TEXT -> try {
          expectedInput.addProperty(KeyConstants.DEFAULT, inputElement.asJsonObject.get(KeyConstants.DEFAULT).asString)
        } catch (exception: Exception) {
          throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
        }
        TypeConstants.NUMBER -> try {
          expectedInput.addProperty(KeyConstants.DEFAULT, inputElement.asJsonObject.get(KeyConstants.DEFAULT).asLong)
        } catch (exception: Exception) {
          throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
        }
        TypeConstants.DECIMAL -> try {
          expectedInput.addProperty(KeyConstants.DEFAULT, inputElement.asJsonObject.get(KeyConstants.DEFAULT).asDouble)
        } catch (exception: Exception) {
          throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
        }
        TypeConstants.BOOLEAN -> try {
          expectedInput.addProperty(KeyConstants.DEFAULT, inputElement.asJsonObject.get(KeyConstants.DEFAULT).asBoolean)
        } catch (exception: Exception) {
          throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
        }
        TypeConstants.FORMULA -> {
        }
        else -> try {
          expectedInput.addProperty(KeyConstants.DEFAULT, inputElement.asJsonObject.get(KeyConstants.DEFAULT).asString)
        } catch (exception: Exception) {
          throw CustomJsonException("{inputs: {$inputName: {${KeyConstants.DEFAULT}: 'Unexpected value for parameter'}}}")
        }
      }
    }
    expectedInputs.add(inputName, expectedInput)
  }
  return expectedInputs
}

fun validateCircuitComputations(computations: JsonObject, inputs: JsonObject, functions: Set<Function>, circuits: Set<Circuit>): Pair<JsonObject, JsonObject> {
  val expectedComputations = JsonObject()
  for ((computationName, computationObject) in computations.entrySet()) {
    val expectedComputation = JsonObject()
    val computation: JsonObject = try {
      computationObject.asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{computations: {$computationName: 'Unexpected value for parameter'}}")
    }
    if (!keyIdentifierPattern.matcher(computationName).matches())
      throw CustomJsonException("{computations: {$computationName: 'Unexpected value for parameter'}}")
    if (!computation.has(ORDER))
      throw CustomJsonException("{computations: {$computationName: {${ORDER}: 'Field is missing in request body'}}}")
    else {
      try {
        expectedComputation.addProperty(ORDER, computation.get(ORDER).asInt)
      } catch (exception: Exception) {
        throw CustomJsonException("{computations: {$computationName: {${ORDER}: 'Unexpected value for parameter'}}}")
      }
    }
    val computationType: String = if (!computation.has(KeyConstants.KEY_TYPE))
      throw CustomJsonException("{computations: {$computationName: {${KeyConstants.KEY_TYPE}: 'Field is missing in request body'}}}")
    else {
      try {
        computation.get(KeyConstants.KEY_TYPE).asString
      } catch (exception: Exception) {
        throw CustomJsonException("{computations: {$computationName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
      }
    }
    expectedComputation.addProperty(KeyConstants.KEY_TYPE, computationType)
    when (computationType) {
      "Function" -> {
        val computationFunction: Function = if (!computation.has(EXECUTE))
          throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Field is missing in request body'}}}")
        else {
          val functionName: String = try {
            computation.get(EXECUTE).asString
          } catch (exception: Exception) {
            throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
          }
          expectedComputation.addProperty(EXECUTE, functionName)
          try {
            functions.single { it.name == functionName }
          } catch (exception: Exception) {
            throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
          }
        }
        if (!computation.has(CONNECT))
          throw CustomJsonException("{computations: {$computationName: {${CONNECT}: 'Field is missing in request body'}}}")
        else {
          val connections: JsonObject = try {
            computation.get(CONNECT).asJsonObject
          } catch (exception: Exception) {
            throw CustomJsonException("{computations: {$computationName: {${CONNECT}: 'Unexpected value for parameter'}}}")
          }
          val expectedConnections = JsonObject()
          for (functionInput in computationFunction.inputs) {
            val connectionName: String = functionInput.name
            if (!connections.has(functionInput.name))
              throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Field is missing in request body'}}}}")
            val connectionsArray: JsonArray = try {
              connections.get(functionInput.name).asJsonArray
            } catch (exception: Exception) {
              throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
            }
            val expectedConnectionsArray = JsonArray()
            val first: String = try {
              connectionsArray.first().asString
            } catch (exception: Exception) {
              throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
            }
            expectedConnectionsArray.add(first)
            when (first) {
              INPUT -> {
                val inputName: String = try {
                  connectionsArray[1].asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                }
                if (!inputs.has(inputName) || inputs.get(inputName).asJsonObject.get(KeyConstants.KEY_TYPE).asString != functionInput.type.name)
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                expectedConnectionsArray.add(inputName)
              }
              COMPUTATION -> {
                val referencedComputationName: String = try {
                  connectionsArray[1].asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                }
                expectedConnectionsArray.add(referencedComputationName)
                val functionOutputName: String = try {
                  connectionsArray[2].asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                }
                expectedConnectionsArray.add(functionOutputName)
              }
              else -> throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
            }
            expectedConnections.add(connectionName, expectedConnectionsArray)
          }
          expectedComputation.add(CONNECT, expectedConnections)
        }
      }
      "Circuit" -> {
        val computationCircuit: Circuit = if (!computation.has(EXECUTE))
          throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Field is missing in request body'}}}")
        else {
          val circuitName: String = try {
            computation.get(EXECUTE).asString
          } catch (exception: Exception) {
            throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
          }
          expectedComputation.addProperty(EXECUTE, circuitName)
          try {
            circuits.single { it.name == circuitName }
          } catch (exception: Exception) {
            throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
          }
        }
        if (!computation.has(CONNECT))
          throw CustomJsonException("{computations: {$computationName: {${CONNECT}: 'Field is missing in request body'}}}")
        else {
          val connections: JsonObject = try {
            computation.get(CONNECT).asJsonObject
          } catch (exception: Exception) {
            throw CustomJsonException("{computations: {$computationName: {${CONNECT}: 'Unexpected value for parameter'}}}")
          }
          val expectedConnections = JsonObject()
          for (circuitInput in computationCircuit.inputs) {
            val connectionName: String = circuitInput.name
            if (!connections.has(circuitInput.name))
              throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Field is missing in request body'}}}}")
            val connectionsArray: JsonArray = try {
              connections.get(circuitInput.name).asJsonArray
            } catch (exception: Exception) {
              throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
            }
            val expectedConnectionsArray = JsonArray()
            val first: String = try {
              connectionsArray.first().asString
            } catch (exception: Exception) {
              throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
            }
            expectedConnectionsArray.add(first)
            when (first) {
              INPUT -> {
                val inputName: String = try {
                  connectionsArray[1].asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                }
                if (!inputs.has(inputName) || inputs.get(inputName).asJsonObject.get(KeyConstants.KEY_TYPE).asString != circuitInput.type.name)
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                expectedConnectionsArray.add(inputName)
              }
              COMPUTATION -> {
                val referencedComputationName: String = try {
                  connectionsArray[1].asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                }
                expectedConnectionsArray.add(referencedComputationName)
                val functionOutputName: String = try {
                  connectionsArray[2].asString
                } catch (exception: Exception) {
                  throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                }
                expectedConnectionsArray.add(functionOutputName)
              }
              else -> throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
            }
            expectedConnections.add(connectionName, expectedConnectionsArray)
          }
          expectedComputation.add(CONNECT, expectedConnections)
        }
      }
      else -> throw CustomJsonException("{computations: {$computationName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
    }
    expectedComputations.add(computationName, expectedComputation)
  }
  // Compute computation levels
  val expectedComputationsWithLevel = JsonObject()
  val computationLevels: MutableMap<String, Int> = mutableMapOf()
  for ((computationName, computationObject) in expectedComputations.entrySet().sortedWith(compareBy { it.value.asJsonObject.get(ORDER).asInt })) {
    val computation: JsonObject = computationObject.asJsonObject
    when (computation.get(KeyConstants.KEY_TYPE).asString) {
      "Function" -> {
        val function: Function = functions.single { it.name == computation.get(EXECUTE).asString }
        val connections: JsonObject = computation.get(CONNECT).asJsonObject
        for (functionInput in function.inputs) {
          val connectionName: String = functionInput.name
          val connectionsArray: JsonArray = connections.get(functionInput.name).asJsonArray
          if (connectionsArray.first().asString == COMPUTATION) {
            val referencedComputationName: String = connectionsArray[1].asString
            if (referencedComputationName in computationLevels) {
              val referencedComputation: JsonObject = expectedComputations.get(referencedComputationName).asJsonObject
              when(referencedComputation.get(KeyConstants.KEY_TYPE).asString) {
                "Function" -> {
                  val referencedComputationFunction: Function = try {
                    functions.single { it.name == referencedComputation.get(EXECUTE).asString }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
                  }
                  val referencedComputationFunctionOutputName: String = connectionsArray[2].asString
                  try {
                    referencedComputationFunction.outputs.single { it.name == referencedComputationFunctionOutputName && it.type == functionInput.type }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                  }
                }
                "Circuit" -> {
                  val referencedComputationCircuit: Circuit = try {
                    circuits.single { it.name == referencedComputation.get(EXECUTE).asString }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
                  }
                  val referencedComputationCircuitOutputName: String = connectionsArray[2].asString
                  try {
                    referencedComputationCircuit.outputs.single { it.name == referencedComputationCircuitOutputName && getCircuitOutputType(it) == functionInput.type }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                  }
                }
              }
            } else throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
          }
        }
      }
      "Circuit" -> {
        val circuit: Circuit = circuits.single { it.name == computation.get(EXECUTE).asString }
        val connections: JsonObject = computation.get(CONNECT).asJsonObject
        for (circuitInput in circuit.inputs) {
          val connectionName: String = circuitInput.name
          val connectionsArray: JsonArray = connections.get(circuitInput.name).asJsonArray
          if (connectionsArray.first().asString == COMPUTATION) {
            val referencedComputationName: String = connectionsArray[1].asString
            if (referencedComputationName in computationLevels) {
              val referencedComputation: JsonObject = expectedComputations.get(referencedComputationName).asJsonObject
              when(referencedComputation.get(KeyConstants.KEY_TYPE).asString) {
                "Function" -> {
                  val referencedComputationFunction: Function = try {
                    functions.single { it.name == referencedComputation.get(EXECUTE).asString }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
                  }
                  val referencedComputationFunctionOutputName: String = connectionsArray[2].asString
                  try {
                    referencedComputationFunction.outputs.single { it.name == referencedComputationFunctionOutputName && it.type == circuitInput.type }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                  }
                }
                "Circuit" -> {
                  val referencedComputationCircuit: Circuit = try {
                    circuits.single { it.name == referencedComputation.get(EXECUTE).asString }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${EXECUTE}: 'Unexpected value for parameter'}}}")
                  }
                  val referencedComputationCircuitOutputName: String = connectionsArray[2].asString
                  try {
                    referencedComputationCircuit.outputs.single { it.name == referencedComputationCircuitOutputName && getCircuitOutputType(it) == circuitInput.type }
                  } catch (exception: Exception) {
                    throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
                  }
                }
              }
            } else throw CustomJsonException("{computations: {$computationName: {${CONNECT}: {${connectionName}: 'Unexpected value for parameter'}}}}")
          }
        }
      }
    }
    val connectionLevels: List<Int> = computation.get(CONNECT).asJsonObject.entrySet()
        .filter { (_, connectionArray) -> connectionArray.asJsonArray.first().asString == COMPUTATION }
        .map { (_, connectionArray) -> computationLevels[connectionArray.asJsonArray[1].asString]!! }
    val level: Int = if (connectionLevels.isEmpty()) 0 else 1 + connectionLevels.maxOrNull()!!
    computationLevels[computationName] = level
    expectedComputationsWithLevel.add(computationName, computationObject.asJsonObject.apply { addProperty(LEVEL, level) })
  }
  // Check mutation of input in more than one computation
  for ((inputName, _) in inputs.entrySet()) {
    var isMutated = false
    var dependentCount = 0
    expectedComputationsWithLevel.entrySet().filter { (_, computationObject) ->
      computationObject.asJsonObject.get(CONNECT).asJsonObject.entrySet().any { (_, referencingConnectionObject) ->
        val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
        referencingConnectionsArray[0].asString == INPUT && referencingConnectionsArray[1].asString == inputName
      }
    }.forEach { (_, referencingComputationObject) ->
      val referencingComputation: JsonObject = referencingComputationObject.asJsonObject
      referencingComputation.get(CONNECT).asJsonObject.entrySet().filter { (_, referencingConnectionObject) ->
        val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
        referencingConnectionsArray[0].asString == INPUT && referencingConnectionsArray[1].asString == inputName
      }.forEach { (referencingConnectionName, _) ->
        dependentCount += 1
        when (referencingComputation.get(KeyConstants.KEY_TYPE).asString) {
          "Function" -> {
            val referencingComputationFunction: Function = functions.single { it.name == referencingComputation.get(EXECUTE).asString }
            val referencingConnectionFunctionInput: FunctionInput = referencingComputationFunction.inputs.single { it.name == referencingConnectionName }
            if (referencingConnectionFunctionInput.variableName != null || referencingConnectionFunctionInput.values != null) isMutated = true
          }
          "Circuit" -> {
            val referencingComputationCircuit: Circuit = circuits.single { it.name == referencingComputation.get(EXECUTE).asString }
            val referencingComputationCircuitInput: CircuitInput = referencingComputationCircuit.inputs.single { it.name == referencingConnectionName }
            val (circuitInputIsMutated, circuitInputDependentCount) = isCircuitInputMutated(referencingComputationCircuitInput, dependentCount)
            isMutated = circuitInputIsMutated
            dependentCount = circuitInputDependentCount
          }
        }
        if (isMutated && dependentCount > 1)
          throw CustomJsonException("{inputs: {${inputName}: 'Input that is mutated cannot be used as input in more than one place'}}")
      }
    }
  }
  // Throw exception if an output is used in more than one place but also mutated in any of them
  for ((computationName, computationObject) in expectedComputationsWithLevel.entrySet().sortedWith(compareBy { it.value.asJsonObject.get(ORDER).asInt })) {
    val computation: JsonObject = computationObject.asJsonObject
    when (computation.get(KeyConstants.KEY_TYPE).asString) {
      "Function" -> {
        val function: Function = functions.single { it.name == computation.get(EXECUTE).asString }
        for (functionOutput in function.outputs) {
          if (functionOutput.type.name !in primitiveTypes) {
            var isMutated = false
            var dependentCount = 0
            expectedComputationsWithLevel.entrySet().filter {
              it.value.asJsonObject.get(CONNECT).asJsonObject.entrySet().any { (_, referencingConnectionObject) ->
                val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
                referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName && referencingConnectionsArray[2].asString == functionOutput.name
              }
            }.sortedWith(compareBy { it.value.asJsonObject.get(ORDER).asLong }).forEach { (referencingComputationName, referencingComputationObject) ->
              val referencingComputation: JsonObject = referencingComputationObject.asJsonObject
              when (referencingComputation.get(KeyConstants.KEY_TYPE).asString) {
                "Function" -> {
                  val referencingComputationFunction: Function = functions.single { function -> function.name == referencingComputation.get(EXECUTE).asString }
                  referencingComputation.get(CONNECT).asJsonObject.entrySet().filter { (_, referencingConnectionObject) ->
                    val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
                    referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName && referencingConnectionsArray[2].asString == functionOutput.name
                  }
                      .forEach { (referencingConnectionName, _) ->
                        dependentCount += 1
                        val referencingConnectionFunctionInput: FunctionInput = referencingComputationFunction.inputs.single { functionInput -> functionInput.name == referencingConnectionName }
                        if (referencingConnectionFunctionInput.variableName != null || referencingConnectionFunctionInput.values != null) isMutated = true
                        if (isMutated && dependentCount > 1)
                          throw CustomJsonException("{computations: {$referencingComputationName: {${CONNECT}: {${referencingConnectionName}: 'Computation output that is mutated cannot be used as input in more than one place'}}}}")
                      }
                }
                "Circuit" -> {
                  val referencingComputationCircuit: Circuit = circuits.single { circuit -> circuit.name == referencingComputation.get(EXECUTE).asString }
                  referencingComputation.get(CONNECT).asJsonObject.entrySet().filter { (_, referencingConnectionObject) ->
                    val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
                    referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName && referencingConnectionsArray[2].asString == functionOutput.name
                  }
                      .forEach { (referencingConnectionName, _) ->
                        val referencingConnectionCircuitInput: CircuitInput = referencingComputationCircuit.inputs.single { circuitInput -> circuitInput.name == referencingConnectionName }
                        val (circuitInputIsMutated, circuitInputDependentCount) = isCircuitInputMutated(referencingConnectionCircuitInput, dependentCount)
                        isMutated = circuitInputIsMutated
                        dependentCount = circuitInputDependentCount
                        if (isMutated && dependentCount > 1)
                          throw CustomJsonException("{computations: {$referencingComputationName: {${CONNECT}: {${referencingConnectionName}: 'Computation output that is mutated cannot be used as input in more than one place'}}}}")
                      }
                }
              }
            }
          }
        }
      }
      "Circuit" -> {
        val circuit: Circuit = circuits.single { it.name == computation.get(EXECUTE).asString }
        for (circuitOutput in circuit.outputs) {
          if (getCircuitOutputType(circuitOutput).name !in primitiveTypes) {
            var isMutated = false
            var dependentCount = 0
            expectedComputationsWithLevel.entrySet().filter {
              it.value.asJsonObject.get(CONNECT).asJsonObject.entrySet().any { (_, referencingConnectionObject) ->
                val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
                referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName && referencingConnectionsArray[2].asString == circuitOutput.name
              }
            }.sortedWith(compareBy { it.value.asJsonObject.get(ORDER).asLong }).forEach { (referencingComputationName, referencingComputationObject) ->
              val referencingComputation: JsonObject = referencingComputationObject.asJsonObject
              when (referencingComputation.get(KeyConstants.KEY_TYPE).asString) {
                "Function" -> {
                  val referencingComputationFunction: Function = functions.single { function -> function.name == referencingComputation.get(EXECUTE).asString }
                  referencingComputation.get(CONNECT).asJsonObject.entrySet().filter { (_, referencingConnectionObject) ->
                    val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
                    referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName && referencingConnectionsArray[2].asString == circuitOutput.name
                  }
                      .forEach { (referencingConnectionName, _) ->
                        dependentCount += 1
                        val referencingConnectionFunctionInput: FunctionInput = referencingComputationFunction.inputs.single { functionInput -> functionInput.name == referencingConnectionName }
                        if (referencingConnectionFunctionInput.variableName != null || referencingConnectionFunctionInput.values != null) isMutated = true
                        if (isMutated && dependentCount > 1)
                          throw CustomJsonException("{computations: {$referencingComputationName: {${CONNECT}: {${referencingConnectionName}: 'Computation output that is mutated cannot be used as input in more than one place'}}}}")
                      }
                }
                "Circuit" -> {
                  val referencingComputationCircuit: Circuit = circuits.single { circuit -> circuit.name == referencingComputation.get(EXECUTE).asString }
                  referencingComputation.get(CONNECT).asJsonObject.entrySet().filter { (_, referencingConnectionObject) ->
                    val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
                    referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName && referencingConnectionsArray[2].asString == circuitOutput.name
                  }
                      .forEach { (referencingConnectionName, _) ->
                        val referencingConnectionCircuitInput: CircuitInput = referencingComputationCircuit.inputs.single { circuitInput -> circuitInput.name == referencingConnectionName }
                        val (circuitInputIsMutated, circuitInputDependentCount) = isCircuitInputMutated(referencingConnectionCircuitInput, dependentCount)
                        isMutated = circuitInputIsMutated
                        dependentCount = circuitInputDependentCount
                        if (isMutated && dependentCount > 1)
                          throw CustomJsonException("{computations: {$referencingComputationName: {${CONNECT}: {${referencingConnectionName}: 'Computation output that is mutated cannot be used as input in more than one place'}}}}")
                      }
                }
              }
            }
          }
        }
      }
    }
  }
  // Remove unused inputs
  val prunedInputs = JsonObject()
  for ((inputName, inputElement) in inputs.entrySet()) {
    if (expectedComputationsWithLevel.entrySet().any {
          it.value.asJsonObject.get(CONNECT).asJsonObject.entrySet().any { (_, referencingConnectionObject) ->
            val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
            referencingConnectionsArray[0].asString == INPUT && referencingConnectionsArray[1].asString == inputName
          }
        }) prunedInputs.add(inputName, inputElement)
  }
  return Pair(prunedInputs, expectedComputationsWithLevel)
}

fun getCircuitOutputType(circuitOutput: CircuitOutput): Type {
  return if (circuitOutput.connectedCircuitComputation.connectedToFunction)
    circuitOutput.connectedCircuitComputationFunctionOutput!!.type
  else
    getCircuitOutputType(circuitOutput.connectedCircuitComputationCircuitOutput!!)
}

fun isCircuitInputMutated(circuitInput: CircuitInput, dependentCount: Int): Pair<Boolean, Int> {
  circuitInput.referencingCircuitComputationConnections.forEach { referencingCircuitComputationConnection ->
    when (referencingCircuitComputationConnection.parentComputation.connectedToFunction) {
      true -> {
        val functionInput: FunctionInput = referencingCircuitComputationConnection.functionInput!!
        if (functionInput.variableName != null || functionInput.values != null)
          return Pair(true, dependentCount + 1)
      }
      false -> {
        val result: Pair<Boolean, Int> = isCircuitInputMutated(circuitInput = referencingCircuitComputationConnection.circuitInput!!, dependentCount = dependentCount)
        if (result.first)
          return result
      }
    }
  }
  return Pair(false, dependentCount)
}

fun validateCircuitOutputs(outputs: JsonObject, inputsAndComputations: Pair<JsonObject, JsonObject>, globalTypes: Set<Type>, functions: Set<Function>): Triple<JsonObject, JsonObject, JsonObject> {
  val expectedOutputs = JsonObject()
  val (inputs, computations) = inputsAndComputations
  for ((outputName, outputObject) in outputs.entrySet()) {
    val expectedOutput = JsonObject()
    if (!keyIdentifierPattern.matcher(outputName).matches())
      throw CustomJsonException("{outputs: {$outputName: 'Output name is not a valid identifier'}}")
    val output: JsonObject = try {
      outputObject.asJsonObject
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: 'Unexpected value for parameter'}}")
    }
    val outputTypeName: String = if (!output.has(KeyConstants.KEY_TYPE))
      throw CustomJsonException("{outputs: {$outputName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
    else try {
      output.get(KeyConstants.KEY_TYPE).asString
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
    }
    val outputType: Type = try {
      globalTypes.single { it.name == outputTypeName }
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: {${KeyConstants.KEY_TYPE}: 'Unexpected value for parameter'}}}")
    }
    expectedOutput.addProperty(KeyConstants.KEY_TYPE, outputTypeName)
    val outputConnections: JsonArray = if (!output.has(CONNECT))
      throw CustomJsonException("{outputs: {$outputName: {${CONNECT}: 'Unexpected value for parameter'}}}")
    else try {
      output.get(CONNECT).asJsonArray
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: {${CONNECT}: 'Unexpected value for parameter'}}}")
    }
    val outputConnectionComputationName: String = try {
      outputConnections[0].asString
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: {${CONNECT}: 'Unexpected value for parameter'}}}")
    }
    val outputConnectionComputationFunction: Function = if (!computations.has(outputConnectionComputationName))
      throw CustomJsonException("{outputs: {$outputName: {${CONNECT}: 'Unexpected value for parameter'}}}")
    else try {
      functions.single { it.name == computations.get(outputConnectionComputationName).asJsonObject.get(EXECUTE).asString }
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: {${CONNECT}: 'Unexpected value for parameter'}}}")
    }
    val outputConnectionComputationFunctionOutputName: String = try {
      outputConnectionComputationFunction.outputs.single { it.name == outputConnections[1].asString && it.type == outputType }.name
    } catch (exception: Exception) {
      throw CustomJsonException("{outputs: {$outputName: {${CONNECT}: 'Unexpected value for parameter'}}}")
    }
    expectedOutput.add(CONNECT, JsonArray().apply {
      add(outputConnectionComputationName)
      add(outputConnectionComputationFunctionOutputName)
    })
    expectedOutputs.add(outputName, output)
  }
  // Remove computations that are referenced neither in outputs nor by any other computation
  val prunedComputations = JsonObject()
  for ((computationName, computationElement) in computations.entrySet()) {
    if (computations.entrySet().any {
          it.value.asJsonObject.get(CONNECT).asJsonObject.entrySet().any { (_, referencingConnectionObject) ->
            val referencingConnectionsArray: JsonArray = referencingConnectionObject.asJsonArray
            referencingConnectionsArray[0].asString == COMPUTATION && referencingConnectionsArray[1].asString == computationName
          }
        }
        || outputs.entrySet().any {
          it.value.asJsonObject.get(CONNECT).asJsonArray.first().asString == computationName
        }
    ) prunedComputations.add(computationName, computationElement)
  }
  return Triple(inputs, prunedComputations, outputs)
}

fun validateCircuitArgs(args: JsonObject, inputs: Set<CircuitInput>): JsonObject {
  val expectedJson = JsonObject()
  for (input in inputs) {
    when (input.type.name) {
      TypeConstants.TEXT -> try {
        expectedJson.addProperty(input.name, if (args.has(input.name)) args.get(input.name).asString else input.defaultStringValue!!)
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.NUMBER -> try {
        expectedJson.addProperty(input.name, if (args.has(input.name)) args.get(input.name).asLong else input.defaultLongValue!!)
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.DECIMAL -> try {
        expectedJson.addProperty(input.name, if (args.has(input.name)) args.get(input.name).asDouble else input.defaultDoubleValue!!)
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.BOOLEAN -> try {
        expectedJson.addProperty(input.name, if (args.has(input.name)) args.get(input.name).asBoolean else input.defaultBooleanValue!!)
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
      TypeConstants.FORMULA -> {
      }
      else -> try {
        expectedJson.addProperty(input.name, if (args.has(input.name)) args.get(input.name).asString else input.referencedVariable!!.name)
      } catch (exception: Exception) {
        throw CustomJsonException("{args: {${input.name}: 'Unexpected value for parameter'}}")
      }
    }
  }
  return expectedJson
}
