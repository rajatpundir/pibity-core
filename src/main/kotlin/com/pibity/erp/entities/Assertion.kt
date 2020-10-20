/* 
 * Copyright (C) 2020 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.erp.entities

import com.google.gson.annotations.Expose
import com.pibity.erp.entities.embeddables.AssertionId
import java.io.Serializable
import java.util.*
import javax.persistence.Column
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "assertion", schema = "inventory")
data class Assertion(

    @EmbeddedId
    val id: AssertionId,

    @Expose
    @Column(name = "display_name", nullable = false)
    var displayName: String = "",

    @Column(name = "expression", nullable = false)
    val expression: String

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as Assertion
    return this.id == other.id
  }

  override fun hashCode(): Int = Objects.hash(id)
}
