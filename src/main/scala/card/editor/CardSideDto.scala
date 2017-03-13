package card.editor

/**
  * Created by bjornkri on 13.03.2017.
  */
private[card] trait CardSideDto {
    def term: String
    def description: Option[String]
}
