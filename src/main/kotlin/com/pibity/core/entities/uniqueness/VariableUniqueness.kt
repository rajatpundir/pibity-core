/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.uniqueness

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Variable
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(
  name = "variable_uniqueness",
  schema = ApplicationConstants.SCHEMA,
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["type_uniqueness_id", "variable_id"]),
    UniqueConstraint(columnNames = ["type_uniqueness_id", "level", "hash"])
  ]
)
data class VariableUniqueness(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "variable_uniqueness_generator")
  @SequenceGenerator(name = "variable_uniqueness_generator", sequenceName = "variable_uniqueness_sequence")
  @Column(name = "id", updatable = false, nullable = false)
  val id: Long = -1L,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "type_uniqueness_id", referencedColumnName = "id"))
  val typeUniqueness: TypeUniqueness,

  @Column(name = "level", nullable = false)
  var level: Int = 0,

  @Column(name = "hash", nullable = false)
  var hash: String,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "variable_id", referencedColumnName = "id"))
  val variable: Variable,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "next_variable_uniqueness_id", referencedColumnName = "id"))
  var nextVariableUniqueness: VariableUniqueness? = null,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "created", nullable = false)
  val created: Timestamp,

  @Column(name = "updated")
  var updated: Timestamp? = null

) : Serializable {

  @PreUpdate
  fun onUpdate() {
    updated = Timestamp(System.currentTimeMillis())
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as VariableUniqueness
    return this.typeUniqueness == other.typeUniqueness && this.hash == other.hash
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
