/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.accumulator

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Key
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

@Entity
@Table(name = "value_accumulator", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["variable_accumulator_id", "key_id"])])
data class ValueAccumulator(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_accumulator_generator")
  @SequenceGenerator(name = "type_accumulator_generator", sequenceName = "type_accumulator_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "variable_accumulator_id", referencedColumnName = "id"))
  val variableAccumulator: VariableAccumulator,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "key_id", referencedColumnName = "id"))
  val key: Key,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

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
    other as ValueAccumulator
    return this.variableAccumulator == other.variableAccumulator && this.key == other.key
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
