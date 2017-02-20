package deck.remover

import deck.editor.creator.ControllerSpec
import org.http4s.{Method, Status, Uri}

class ApiSpec extends ControllerSpec {
    private val bbbaseUri = Uri.fromString("http://localhost:8070/api/decks").valueOr(e => throw e)

    "DELETE/:id" when {
        "valid id" should {
            "give 2XX No Content" in {
                val uri = bbbaseUri / "sum crazy id"
                val response = executeRequest(Method.DELETE, uri)
                assert(response.status == Status.NoContent)
            }
        }

        "invalid UUID" should {
            "give 402 Bad Request w/ error message" ignore {

            }
        }

        "no deck with id" should {
            "give 404 Not Found w/ error message" ignore {

            }
        }
    }
}
