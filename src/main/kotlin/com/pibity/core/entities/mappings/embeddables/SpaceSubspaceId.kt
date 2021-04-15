/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.entities.mappings.embeddables

import com.pibity.core.entities.Space
import com.pibity.core.entities.Subspace
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne

@Embeddable
data class SpaceSubspaceId(

  @ManyToOne
  @JoinColumns(JoinColumn(name = "space_id", referencedColumnName = "id"))
  val space: Space,

  @ManyToOne
  @JoinColumns(JoinColumn(name = "subspace_id", referencedColumnName = "id"))
  val subspace: Subspace

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as SpaceSubspaceId
    return this.space == other.space && this.subspace == other.subspace
  }

  override fun hashCode(): Int = Objects.hash(space, subspace)
}
