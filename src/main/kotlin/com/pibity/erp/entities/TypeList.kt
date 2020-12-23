/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import java.io.Serializable
import java.sql.Timestamp
import javax.persistence.*

@Entity
@Table(name = "type_list", schema = "inventory")
data class TypeList(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "type_list_generator")
    @SequenceGenerator(name="type_list_generator", sequenceName = "type_list_sequence")
    val id: Long = -1,

    @Version
    @Column(name = "version", nullable = false)
    val version: Timestamp = Timestamp(System.currentTimeMillis()),

    @Column(name = "min", nullable = false)
    val min: Int,

    @Column(name = "max", nullable = false)
    val max: Int,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "type_id", referencedColumnName = "id")])
    var type: Type

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as TypeList
    return this.id == other.id
  }

  override fun hashCode(): Int = (id % Int.MAX_VALUE).toInt()
}
