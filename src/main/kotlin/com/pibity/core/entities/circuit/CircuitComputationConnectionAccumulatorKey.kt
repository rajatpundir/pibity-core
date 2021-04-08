/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.circuit

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Key
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "circuit_computation_connection_accumulator_key", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["parent_circuit_computation_connection_id", "key_id"])])
data class CircuitComputationConnectionAccumulatorKey(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_computation_connection_accumulator_key_generator")
    @SequenceGenerator(name = " circuit_computation_connection_accumulator_key_generator", sequenceName = "circuit_computation_connection_accumulator_key_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "parent_circuit_computation_connection_id", nullable = false)
    val parentComputationConnection: CircuitComputationConnection,

    @ManyToOne
    @JoinColumns(JoinColumn(name = "key_id", referencedColumnName = "id"))
    val key: Key,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

    @ManyToOne
    @JoinColumn(name = "circuit_input_id")
    val circuitInput: CircuitInput,

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
        other as CircuitComputationConnectionAccumulatorKey
        return this.parentComputationConnection == other.parentComputationConnection && this.key == other.key
    }

    override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
