package common

import java.util.UUID

object UuidParser {
    def apply(id: String): Either[ErrorMessage, UUID] =
        try {
            Right(UUID.fromString(id))
        }
        catch {
            case e: IllegalArgumentException => Left(InvalidUuidFormat(id))
        }
}
