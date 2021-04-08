/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.circuit

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.accumulator.TypeAccumulator
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.function.FunctionOutput
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
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
    val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

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

    @ManyToOne
    @JoinColumn(name = "connected_circuit_computation_type_accumulator_id")
    val connectedCircuitComputationTypeAccumulator: TypeAccumulator? = null,

    @OneToMany(mappedBy = "parentComputationConnection", cascade = [CascadeType.PERSIST, CascadeType.MERGE], orphanRemoval = true)
    val connectedCircuitComputationAccumulatorKeys: MutableSet<CircuitComputationConnectionAccumulatorKey> = HashSet(),

    @Column(name = "created", nullable = false)
    val created: Timestamp,

    @Column(name = "updated")
    var updated: Timestamp? = null

) : Serializable {

    @PreUpdate
    fun onUpdate() {
        updated = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime())
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true
        other as CircuitComputationConnection
        return this.parentComputation == other.parentComputation && this.functionInput == other.functionInput && this.circuitInput == other.circuitInput
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
