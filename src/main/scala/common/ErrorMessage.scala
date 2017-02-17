package common

import java.util.UUID

trait ErrorMessage {
    def message: String
}

case class GeneralError(errorMessage: String) extends ErrorMessage {
    def message: String = errorMessage
}

case class InvalidIdFormat(id: String) extends ErrorMessage {
    def message: String = "Invalid UUID format: " + id
}

case class InvalidId(entity: String, id: UUID) extends ErrorMessage {
    def message: String = "Could not find " + entity + " with ID: " + id
}

case class CannotBeEmpty(field: String) extends ErrorMessage {
    def message: String = "Parameter \"" + field + "\" cannot be empty."
}

object DatabaseError extends ErrorMessage {
    def message: String = "Database error. Please consult logs."
}
