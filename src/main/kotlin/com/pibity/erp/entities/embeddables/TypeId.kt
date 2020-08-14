/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities.embeddables

import com.pibity.erp.entities.Organization
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@Embeddable
data class TypeId(

    @OneToOne
    @JoinColumn(name = "organization_id", nullable = false)
    val organization: Organization,

    @Column(name = "super_type_name")
    val superTypeName: String,

    @Column(name = "type_name", nullable = false)
    val name: String = ""

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as TypeId
    return this.organization == other.organization && this.superTypeName == other.superTypeName && this.name == other.name
  }

  override fun hashCode(): Int = Objects.hash(organization, superTypeName, name)
}