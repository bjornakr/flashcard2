package common

trait ErrorMessage {
    def message: String
}

case class SpesificError(errorMessage: String) extends ErrorMessage {
    def message: String = errorMessage
}

case class InvalidUuidFormat(id: String) extends ErrorMessage {
    def message: String = s"""Invalid UUID format: "$id"."""
}

case class CouldNotFindEntityWithId(entity: String, id: String) extends ErrorMessage {
    def message: String = s"Could not find $entity with ID: $id."
}

case class CannotBeEmpty(field: String) extends ErrorMessage {
    def message: String = s"""Parameter "$field" cannot be empty."""
}

case class CouldNotParse(source: String, into: Any) extends ErrorMessage {
    def message: String = s"Could not parse $source into ${into.getClass.getCanonicalName}."
}

object DatabaseError extends ErrorMessage {
    def message: String = "Database error. Please consult logs."
}
