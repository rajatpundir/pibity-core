/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.assertion.VariableAssertion
import com.pibity.core.entities.uniqueness.VariableUniqueness
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "variable", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["subspace_id", "type_id", "name"])])
data class Variable(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "variable_generator")
  @SequenceGenerator(name = "variable_generator", sequenceName = "variable_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumn(name = "subspace_id", nullable = false)
  val subspace: Subspace,

  @ManyToOne
  @JoinColumn(name = "type_id", nullable = false)
  val type: Type,

  @Column(name = "name", nullable = false)
  var name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @Column(name = "active", nullable = false)
  var active: Boolean = true,

  @OneToMany(mappedBy = "variable", cascade = [CascadeType.ALL], orphanRemoval = true)
  val values: MutableSet<Value> = HashSet(),

  @OneToMany(mappedBy = "variable", cascade = [CascadeType.ALL], orphanRemoval = true)
  val variableUniqueness: MutableSet<VariableUniqueness> = HashSet(),

  @OneToMany(mappedBy = "variable", cascade = [CascadeType.ALL], orphanRemoval = true)
  val variableAssertions: MutableSet<VariableAssertion> = HashSet(),

  @OneToMany(mappedBy = "referencedVariable", cascade = [CascadeType.ALL])
  val referencingValues: Set<Value> = HashSet(),

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
    other as Variable
    return this.subspace == other.subspace && this.type == other.type && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
