package views.html
package tournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object faq {

  def page(system: Option[lidraughts.tournament.System])(implicit ctx: Context) = views.html.base.layout(
    title = trans.tournamentFAQ.txt(),
    moreCss = cssTag("page")
  ) {
      main(cls := "page-small box box-pad page")(
        h1(
          a(href := routes.Tournament.home(), dataIcon := "I", cls := "text"),
          trans.tournamentFAQ()
        ),
        div(cls := "body")(apply(system = system))
      )
    }

  def apply(rated: Option[Boolean] = None, system: Option[lidraughts.tournament.System] = None, privateId: Option[String] = None)(implicit ctx: Context) = frag(
    privateId.map { id =>
      frag(
        h2(trans.arena.thisIsPrivate()),
        p(trans.arena.shareUrl(s"$netBaseUrl${routes.Tournament.show(id)}")) // XXX
      )
    },
    p(trans.arena.willBeNotified()),

    h2(trans.arena.isItRated()),
    rated match {
      case Some(true) => p(trans.arena.isRated())
      case Some(false) => p(trans.arena.isNotRated())
      case None => p(trans.arena.someRated())
    },

    h2(trans.arena.howAreScoresCalculated()),
    p(trans.arena.howAreScoresCalculatedAnswer()),

    h2(trans.arena.berserk()),
    p(trans.arena.berserkAnswer()),

    h2(trans.arena.howIsTheWinnerDecided()),
    p(trans.arena.howIsTheWinnerDecidedAnswer()),

    h2(trans.arena.howDoesPairingWork()),
    p(trans.arena.howDoesPairingWorkAnswer()),

    h2(trans.arena.howDoesItEnd()),
    p(trans.arena.howDoesItEndAnswer()),

    h2(trans.arena.otherRules()),
    p(trans.arena.thereIsACountdown()),
    p(trans.arena.drawingWithinNbMoves.pluralSame(10))
  )
}
