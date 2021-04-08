/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.permission

import com.pibity.core.commons.constants.ApplicationConstants
import com.pibity.core.entities.function.FunctionInput
import java.io.Serializable
import java.sql.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.persistence.*

@Entity
@Table(name = "function_input_permission", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["function_permission_id", "function_input_id"])])
data class FunctionInputPermission(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_input_permission_generator")
  @SequenceGenerator(name = "function_input_permission_generator", sequenceName = "function_input_permission_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "function_permission_id", referencedColumnName = "id"))
  val functionPermission: FunctionPermission,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "function_input_id", referencedColumnName = "id"))
  val functionInput: FunctionInput,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp.valueOf(ZonedDateTime.now(ZoneId.of("Etc/UTC")).toLocalDateTime()),

  @Column(name = "access_level", nullable = false)
  var accessLevel: Boolean,

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
    other as FunctionInputPermission
    return this.functionInput == other.functionInput && this.functionPermission == other.functionPermission
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
