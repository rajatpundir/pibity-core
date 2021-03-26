/*
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.permission

import com.pibity.erp.commons.constants.ApplicationConstants
import com.pibity.erp.entities.function.FunctionOutput
import java.io.Serializable
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "function_output_permission", schema = ApplicationConstants.SCHEMA, uniqueConstraints = [UniqueConstraint(columnNames = ["function_permission_id", "function_output_id"])])
data class FunctionOutputPermission(

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "function_output_permission_generator")
  @SequenceGenerator(name="function_output_permission_generator", sequenceName = "function_output_permission_sequence")
  val id: Long = -1,

  @ManyToOne
  @JoinColumns(*[JoinColumn(name = "function_permission_id", referencedColumnName = "id")])
  val functionPermission: FunctionPermission,

  @ManyToOne
  @JoinColumns(*[JoinColumn(name = "function_output_id", referencedColumnName = "id")])
  val functionOutput: FunctionOutput,

  @Version
  @Column(name = "version", nullable = false)
  val version: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "access_level", nullable = false)
  var accessLevel: Boolean,

  @Column(name = "created", nullable = false)
  val created: Timestamp = Timestamp(System.currentTimeMillis()),

  @Column(name = "updated")
  var updated: Timestamp? = null

) : Serializable {

  @PreUpdate
  fun setUpdatedTimestamp() {
    updated = Timestamp(System.currentTimeMillis())
  }

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as FunctionOutputPermission
    return this.functionOutput == other.functionOutput && this.functionPermission == other.functionPermission
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
