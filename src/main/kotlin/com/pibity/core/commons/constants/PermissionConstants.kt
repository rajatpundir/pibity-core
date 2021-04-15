/* 
 * Copyright (C) 2020-2021 Pibity Infotech Private Limited - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * THIS IS UNPUBLISHED PROPRIETARY CODE OF PIBITY INFOTECH PRIVATE LIMITED
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.pibity.core.commons.constants

object PermissionConstants {
  const val PERMISSION_NAME = "permissionName"
  const val PERMISSION_TYPE = "permissionType"
  const val CREATE = "CREATE"
  const val READ = "READ"
  const val UPDATE = "UPDATE"
  const val DELETE = "DELETE"
  const val PUBLIC = "PUBLIC"
}

val permissionTypes = setOf(
  PermissionConstants.CREATE,
  PermissionConstants.READ,
  PermissionConstants.UPDATE,
  PermissionConstants.DELETE,
  PermissionConstants.PUBLIC
)
