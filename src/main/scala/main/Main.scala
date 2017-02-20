package main

import org.http4s.server.Server
import org.http4s.server.blaze._

class Main(
              deckEditorCreator: deck.editor.creator.Controller,
              deckRemover: deck.remover.Controller
          ) {

    def createServer: Server = createServer(8070)

    def createServer(port: Int): Server = {
        val builder = BlazeBuilder.bindHttp(port, "localhost")
            .mountService(deckEditorCreator.httpService, "/api/deck/creator")
            .mountService(deckRemover.httpService, "/api/deck/remover")
        builder.run
    }

}
