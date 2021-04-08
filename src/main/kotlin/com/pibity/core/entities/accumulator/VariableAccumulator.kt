/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.accumulator

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Variable
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "variable_accumulator", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["type_accumulator_id", "level", "hash"])])
data class VariableAccumulator(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_accumulator_generator")
  @SequenceGenerator(name = "type_accumulator_generator", sequenceName = "type_accumulator_sequence")
  val id: Long = -1,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @ManyToOne
  @JoinColumns(JoinColumn(name = "type_accumulator_id", referencedColumnName = "id"))
  val typeAccumulator: TypeAccumulator,

  @Column(name = "level", nullable = false)
  var level: Int = 0,

  @Column(name = "hash", nullable = false)
  var hash: String,

  @Column(name = "value_string", length = 1024)
  var stringValue: String? = null,

  @Column(name = "value_long")
  var longValue: Long? = null,

  @Column(name = "value_decimal")
  var decimalValue: BigDecimal? = null,

  @Column(name = "value_boolean")
  var booleanValue: Boolean? = null,

  @Column(name = "value_date")
  var dateValue: Date? = null,

  @Column(name = "value_timestamp")
  var timestampValue: Timestamp? = null,

  @Column(name = "value_time")
  var timeValue: Time? = null,

  @Lob
  @Column(name = "value_blob")
  var blobValue: Blob? = null,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "value_referenced_variable_id", referencedColumnName = "id"))
  var referencedVariable: Variable? = null,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "next_variable_accumulator_id", referencedColumnName = "id"))
  var nextVariableAccumulator: VariableAccumulator? = null,

  @ManyToMany
  @JoinTable(name = "mapping_variable_accumulator_values", schema = ApplicationConstants.SCHEMA, joinColumns = [JoinColumn(name = "variable_accumulator_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "value_accumulator_id", referencedColumnName = "id")])
  val values: MutableSet<ValueAccumulator> = HashSet(),

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
    other as VariableAccumulator
    return this.id == other.id
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
