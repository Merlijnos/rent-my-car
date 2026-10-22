package rentmycar.auth

import rentmycar.users.Role
import rentmycar.users.User

data class UserPrincipal(val user: User)

class ForbiddenException(message: String) : Exception(message)

fun UserPrincipal.requireRole(role: Role): User {
    if (user.role != role) {
        throw ForbiddenException("Role $role is required")
    }
    return user
}
