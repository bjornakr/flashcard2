package card.editor

import common.{CannotBeEmpty, ErrorMessage}

// DOMAIN
abstract case class CardSide(term: String, description: Option[String])

object CardSide {
    private def proc(term: String, description: Option[String], errorMessage: String): Either[ErrorMessage, CardSide] = {
        def emptyToNone(s: String) = if (s.trim == "") None else Some(s)
        val nonEmptyDescription: Option[String] = description.flatMap(emptyToNone)

        term.trim match {
            case "" => Left(CannotBeEmpty(errorMessage))
            case t => Right(new CardSide(t, nonEmptyDescription) {})
        }
    }

    def front(term: String, description: Option[String]): Either[ErrorMessage, CardSide] =
        proc(term, description, "Front: Term")


    def back(term: String, description: Option[String]): Either[ErrorMessage, CardSide] =
        proc(term, description, "Back: Term")

}