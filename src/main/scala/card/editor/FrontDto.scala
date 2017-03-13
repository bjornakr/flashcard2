package card.editor

private[card] case class FrontDto(term: String, description: Option[String]) extends CardSideDto
