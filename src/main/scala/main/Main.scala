package main

import deck.editor.creator.Controller
import org.http4s.server.Server
import org.http4s.server.blaze._

class Main(deckEditorCreator: Controller) {

    def createServer: Server = createServer(8070)

    def createServer(port: Int): Server = {
        val builder = BlazeBuilder.bindHttp(port, "localhost")
            .mountService(deckEditorCreator.httpService, "/api/decks")
        builder.run
    }

}
