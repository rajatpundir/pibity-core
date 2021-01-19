/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.circuit

import com.pibity.erp.entities.Organization
import com.pibity.erp.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "circuit", schema = "inventory", uniqueConstraints = [UniqueConstraint(columnNames = ["organization_id", "name"])])
data class Circuit(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "circuit_generator")
    @SequenceGenerator(name = " circuit_generator", sequenceName = "circuit_sequence")
    val id: Long = -1,

    @ManyToOne
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Column(name = "name", nullable = false)
    val name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @OneToMany(mappedBy = "parentCircuit", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val inputs: MutableSet<CircuitInput> = HashSet(),

    @OneToMany(mappedBy = "parentCircuit", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val computations: MutableSet<CircuitComputation> = HashSet(),

    @OneToMany(mappedBy = "parentCircuit", cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    val outputs: MutableSet<CircuitOutput> = HashSet()

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Circuit
    return this.organization == other.organization && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
