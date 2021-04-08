/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.function

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.Key
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_input_key", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["function_input_id", "key_id"])])
data class FunctionInputKey(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_input_key_generator")
  @SequenceGenerator(name="function_input_key_generator", sequenceName = "function_input_key_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "function_input_id", referencedColumnName = "id"))
  val functionInput: FunctionInput,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "key_id", referencedColumnName = "id"))
  val key: Key,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @Lob
  @Column(name = "expression")
  var expression: String,

  @ManyToMany
  @JoinTable(name = "mapping_function_input_key_dependencies", schema = ApplicationConstants.SCHEMA, joinColumns = [JoinColumn(name = "function_input_key_id", referencedColumnName = "id")], inverseJoinColumns = [JoinColumn(name = "dependency_key_id", referencedColumnName = "id")])
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
    other as FunctionInputKey
    return this.functionInput == other.functionInput && this.key == other.key
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
