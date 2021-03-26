/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.circuit

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.function.FunctionInput
import com.pibity.erp.entities.function.FunctionOutput
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "circuit_computation_connection", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["parent_circuit_computation_id", "function_input_id", "circuit_input_id"])])
data class CircuitComputationConnection(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_computation_connection_generator")
    @SequenceGenerator(name = " circuit_computation_connection_generator", sequenceName = "circuit_computation_connection_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "parent_circuit_computation_id", nullable = false)
    val parentComputation: CircuitComputation,

    @ManyToOne
    @JoinColumn(name = "function_input_id")
    val functionInput: FunctionInput? = null,

    @ManyToOne
    @JoinColumn(name = "circuit_input_id")
    val circuitInput: CircuitInput? = null,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @ManyToOne
    @JoinColumn(name = "connected_circuit_input_id")
    val connectedCircuitInput: CircuitInput? = null,

    @ManyToOne
    @JoinColumn(name = "connected_circuit_computation_id")
    val connectedCircuitComputation: CircuitComputation? = null,

    @ManyToOne
    @JoinColumn(name = "connected_circuit_computation_function_output_id")
    val connectedCircuitComputationFunctionOutput: FunctionOutput? = null,

    @ManyToOne
    @JoinColumn(name = "connected_circuit_computation_circuit_output_id")
    val connectedCircuitComputationCircuitOutput: CircuitOutput? = null,

    @Column(name = "created", nullable = false)
    val created: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "updated")
    var updated: Timestamp? = null

) : Serializable {

    @PreUpdate
    fun setUpdatedTimestamp() {
        updated = Timestamp(System.currentTimeMillis())
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true
        other as CircuitComputationConnection
        return this.parentComputation == other.parentComputation && this.functionInput == other.functionInput && this.circuitInput == other.circuitInput
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
