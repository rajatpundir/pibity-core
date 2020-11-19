/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.google.gson.annotations.Expose
import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "organization", schema = "inventory")
data class Organization(

    @Expose
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "organization_generator")
    @SequenceGenerator(name="organization_generator", sequenceName = "organization_sequence")
    @Column(name = "id", updatable = false, nullable = false)
    val id: Long = -1L,

    @Expose
    @Column(name = "name", unique = true, nullable = false)
    var name: String,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @OneToOne
    var superList: VariableList? = null

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Organization
    return this.name == other.name
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
