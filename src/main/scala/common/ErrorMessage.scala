package common

trait ErrorMessage {
    def message: String
}

case class SpesificError(errorMessage: String) extends ErrorMessage {
    def message: String = errorMessage
}

case class InvalidUuidFormat(id: String) extends ErrorMessage {
    def message: String = "Invalid UUID format: " + id
}

case class CouldNotFindEntityWithId(entity: String, id: String) extends ErrorMessage {
    def message: String = "Could not find " + entity + " with ID: " + id
}

case class CannotBeEmpty(field: String) extends ErrorMessage {
    def message: String = "Parameter \"" + field + "\" cannot be empty."
}

object DatabaseError extends ErrorMessage {
    def message: String = "Database error. Please consult logs."
}
