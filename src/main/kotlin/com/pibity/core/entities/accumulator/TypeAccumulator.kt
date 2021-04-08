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
import com.pibity.core.entities.Type
import com.pibity.core.entities.Variable
import com.pibity.core.entities.uniqueness.TypeUniqueness
import java.io.Serializable
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Time
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*
import kotlin.collections.HashSet

@Entity
@Table(name = "type_accumulator", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["type_uniqueness_id", "name"])])
data class TypeAccumulator(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_accumulator_generator")
  @SequenceGenerator(name = "type_accumulator_generator", sequenceName = "type_accumulator_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "type_uniqueness_id", referencedColumnName = "id"))
  val typeUniqueness: TypeUniqueness,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @ManyToMany
  @JoinTable(name = "mapping_type_accumulator_keys", schema = ApplicationConstants.SCHEMA, joinColumns = [JoinColumn(name = "type_accumulator_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "key_id", referencedColumnName = "id")])
  val keys: MutableSet<Key> = HashSet(),

  @ManyToOne
  @JoinColumns(JoinColumn(name = "type_id", referencedColumnName = "id"))
  val type: Type,

  @Column(name = "value_string")
  var initialStringValue: String? = null,

  @Column(name = "value_long")
  var initialLongValue: Long? = null,

  @Column(name = "value_decimal")
  var initialDecimalValue: BigDecimal? = null,

  @Column(name = "value_boolean")
  var initialBooleanValue: Boolean? = null,

  @Column(name = "value_date")
  var initialDateValue: Date? = null,

  @Column(name = "value_timestamp")
  var initialTimestampValue: Timestamp? = null,

  @Column(name = "value_time")
  var initialTimeValue: Time? = null,

  @Lob
  @Column(name = "value_blob")
  var initialBlobValue: Blob? = null,

  @ManyToOne(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
  @JoinColumns(JoinColumn(name = "value_referenced_variable_id", referencedColumnName = "id"))
  var referencedVariable: Variable? = null,

  @Lob
  @Column(name = "symbol_paths", nullable = false)
  var symbolPaths: String,

  @Lob
  @Column(name = "forward_expression", nullable = false)
  val forwardExpression: String,

  @Lob
  @Column(name = "backward_expression", nullable = false)
  val backwardExpression: String,

  @ManyToMany
  @JoinTable(name = "mapping_type_accumulator_key_dependencies", schema = ApplicationConstants.SCHEMA, joinColumns = [JoinColumn(name = "type_accumulator_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "key_id", referencedColumnName = "id")])
  val keyDependencies: MutableSet<Key> = HashSet(),

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
    other as TypeAccumulator
    return this.typeUniqueness == other.typeUniqueness && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
