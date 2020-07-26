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
import java.util.*
import javax.persistence.*

@Entity
@Table(name = "organization", schema = "inventory")
data class Organization(

    @Id
    @Expose
    @Column(name = "id", unique = true, nullable = false)
    var id: String

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Organization
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
