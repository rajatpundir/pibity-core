package com.pibity.erp.entities.embeddables

import com.pibity.erp.entities.Role
import com.pibity.erp.entities.User
import java.io.Serializable
import java.util.*
import javax.persistence.Embeddable
import javax.persistence.JoinColumn
import javax.persistence.JoinColumns
import javax.persistence.ManyToOne

@Embeddable
data class UserRoleId(

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "user_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "username", referencedColumnName = "username")])
    val user: User,

    @ManyToOne
    @JoinColumns(*[JoinColumn(name = "role_organization_id", referencedColumnName = "organization_id"),
      JoinColumn(name = "role_name", referencedColumnName = "role_name")])
    val role: Role

) : Serializable {

  override fun equals(other: Any?): Boolean {
    other ?: return false
    if (this === other) return true
    other as UserRoleId
    return this.user == other.user && this.role == other.role
  }

  override fun hashCode(): Int = Objects.hash(user, role)
}
