/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.pibity.erp.commons.exceptions.CustomJsonException
import java.io.Serializable
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "list_type", schema = "inventory")
data class TypeList(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = -1,

    @Column(name = "min", nullable = false)
    val min: Int,

    @Column(name = "max", nullable = false)
    val max: Int,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "super_type_name", referencedColumnName = "super_type_name"),
      JoinColumn(name = "type_name", referencedColumnName = "type_name")])
    var type: Type

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as TypeList
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
