/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.permission

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.function.Function
import com.pibity.core.entities.function.FunctionInput
import com.pibity.core.entities.function.FunctionOutput
import com.pibity.core.entities.mappings.SpaceFunctionPermission
import com.pibity.core.serializers.serialize
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_permission", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["function_id", "name"])])
data class FunctionPermission(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_permission_generator")
  @SequenceGenerator(name="function_permission_generator", sequenceName = "function_permission_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "function_id", referencedColumnName = "id"))
  val function: Function,

  @Column(name = "name", nullable = false)
  val name: String,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @ManyToMany
  @JoinTable(name = "mapping_function_permission_function_inputs", schema = ApplicationConstants.SCHEMA,
    joinColumns = [JoinColumn(name = "function_permission_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "function_input_id", referencedColumnName = "id")])
  var inputs: Set<FunctionInput> = HashSet(),

  @ManyToMany
  @JoinTable(name = "mapping_function_permission_function_outputs", schema = ApplicationConstants.SCHEMA,
    joinColumns = [JoinColumn(name = "function_permission_id", referencedColumnName = "id")],
    inverseJoinColumns = [JoinColumn(name = "function_output_id", referencedColumnName = "id")])
  var outputs: Set<FunctionOutput> = HashSet(),

  @OneToMany(mappedBy = "id.permission", cascade = [CascadeType.ALL], orphanRemoval = true)
  val permissionSpaces: Set<SpaceFunctionPermission> = HashSet(),

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
    other as FunctionPermission
    return this.function == other.function && this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()

  override fun toString(): String = serialize(this).toString()
}
