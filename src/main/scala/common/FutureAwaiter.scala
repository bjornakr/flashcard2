package common

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object FutureAwaiter {
    def apply[A, B](future: Future[A])(successHandler: (A) => Either[ErrorMessage, B]): Either[ErrorMessage, B] = {
        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Failure(e) => {
                // TODO: Log error
                Left(DatabaseError) // or database error - but do not send error message to api?
            }
            case Success(a) => successHandler(a)
        }
    }
}
