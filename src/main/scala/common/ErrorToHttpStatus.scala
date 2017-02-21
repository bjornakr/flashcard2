package common

import org.http4s.Response
import org.http4s.dsl._

import scalaz.concurrent.Task

object ErrorToHttpStatus {
    def apply(message: ErrorMessage): Task[Response] = {
        message match {
            case CouldNotFindEntityWithId(_, _) => NotFound(message.message)
            case InvalidIdFormat(_) => BadRequest(message.message)
            case CannotBeEmpty(_) => BadRequest(message.message)
            case DatabaseError => InternalServerError(message.message)
            case _ => NotFound(message.message)
        }
    }
}
