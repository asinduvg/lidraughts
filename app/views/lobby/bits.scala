package views.html.lobby

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  val lobbyApp = div(cls := "lobby__app")(
    div(cls := "tabs-horiz")(span(nbsp)),
    div(cls := "lobby__app__content")
  )

  def underboards(
    tours: List[lidraughts.tournament.Tournament],
    simuls: List[lidraughts.simul.Simul],
    leaderboard: List[lidraughts.user.User.LightPerf],
    tournamentWinners: List[lidraughts.tournament.Winner]
  )(implicit ctx: Context) = frag(
    div(cls := "lobby__leaderboard lobby__box")(
      div(cls := "lobby__box__top")(
        span(cls := "title text", dataIcon := "C")(trans.leaderboard()),
        a(cls := "more", href := routes.User.list)(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        table(tbody(
          leaderboard map { l =>
            tr(
              td(lightUserLink(l.user)),
              lidraughts.rating.PerfType(l.perfKey) map { pt =>
                td(cls := "text", dataIcon := pt.iconChar)(l.rating)
              },
              td(ratingProgress(l.progress))
            )
          }
        ))
      )
    ),
    div(cls := "lobby__winners lobby__box")(
      div(cls := "lobby__box__top")(
        span(cls := "title text", dataIcon := "g")(trans.tournamentWinners()),
        a(cls := "more", href := routes.Tournament.leaderboard)(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        table(tbody(
          tournamentWinners take 10 map { w =>
            tr(
              td(userIdLink(w.userId.some)),
              td(a(title := w.tourName, href := routes.Tournament.show(w.tourId))(scheduledTournamentNameShortHtml(w.tourName)))
            )
          }
        ))
      )
    ),
    div(cls := "lobby__tournaments lobby__box")(
      div(cls := "lobby__box__top")(
        span(cls := "title text", dataIcon := "g")(trans.openTournaments()),
        a(cls := "more", href := routes.Tournament.home())(trans.more(), " »")
      ),
      div(id := "enterable_tournaments", cls := "enterable_list lobby__box__content")(
        views.html.tournament.bits.enterable(tours)
      )
    ),
    div(cls := "lobby__simuls lobby__box")(
      div(cls := "lobby__box__top")(
        span(cls := "title text", dataIcon := "f")(trans.simultaneousExhibitions()),
        a(cls := "more", href := routes.Simul.home())(trans.more(), " »")
      ),
      div(id := "enterable_simuls", cls := "enterable_list lobby__box__content")(
        views.html.simul.bits.allCreated(simuls)
      )
    )
  )

  def lastPosts(posts: List[lidraughts.blog.MiniPost])(implicit ctx: Context): Option[Frag] = posts.nonEmpty option
    div(cls := "lobby__blog lobby__box")(
      div(cls := "lobby__box__top")(
        span(cls := "title text", dataIcon := "6")(trans.latestUpdates()),
        a(cls := "more", href := routes.Blog.index())(trans.more(), " »")
      ),
      div(cls := "lobby__box__content")(
        posts map { post =>
          a(cls := "post", href := routes.Blog.show(post.id, post.slug))(
            img(src := post.image),
            span(cls := "text")(
              strong(post.title),
              span(post.shortlede)
            ),
            semanticDate(post.date)
          )
        }
      )
    )

  def playbanInfo(ban: lidraughts.playban.TempBan)(implicit ctx: Context) = nopeInfo(
    h1("Sorry :("),
    p("We had to time you out for a ", (ban.remainingSeconds < 3600) ?? "little ", "while."),
    p("The timeout expires ", strong(secondsFromNow(ban.remainingSeconds)), "."),
    h2("Why?"),
    p(
      "We aim to provide a pleasant draughts experience for everyone.", br,
      "To that effect, we must ensure that all players follow good practice.", br,
      "When a potential problem is detected, we display this message."
    ),
    h2("How to avoid this?"),
    ul(
      li("Play every game you start"),
      li("Try to win (or at least draw) every game you play"),
      li("Resign lost games (don't let the clock run down)")
    ),
    p(
      "We apologize for the temporary inconvenience,", br,
      "and wish you great games on lidraughts.org.", br,
      "Thank you for reading!"
    )
  )

  def currentGameInfo(current: lidraughts.app.mashup.Preload.CurrentGame)(implicit ctx: Context) = nopeInfo(
    h1("Hang on!"),
    p("You have a game in progress with ", strong(current.opponent), "."),
    br, br,
    a(cls := "text button button-fat", dataIcon := "G", href := routes.Round.player(current.pov.fullId))("Join the game"),
    br, br,
    "or",
    br, br,
    form(action := routes.Round.resign(current.pov.fullId), method := "post")(
      button(cls := "text button button-red", dataIcon := "L")(
        if (current.pov.game.abortable) "Abort" else "Resign", " the game"
      )
    ),
    br,
    p("You can't start a new game until this one is finished."),
    br, br,
    p(
      "If you want to play several games simultaneously,",
      br,
      a(href := routes.Simul.home)("create a simultaneous exhibition event"),
      "!"
    )
  )

  def nopeInfo(content: Modifier*) = frag(
    div(cls := "lobby__app"),
    div(cls := "lobby__nope")(
      st.section(cls := "lobby__app__content")(content)
    )
  )

  def spotlight(e: lidraughts.event.Event)(implicit ctx: Context) = a(
    href := routes.Event.show(e.id).url,
    cls := List(
      s"tour-spotlight event-spotlight id_${e.id}" -> true,
      "invert" -> e.isNowOrSoon
    )
  )(
      i(cls := "img", dataIcon := ""),
      span(cls := "content")(
        span(cls := "name")(e.title),
        span(cls := "headline")(e.headline),
        span(cls := "more")(
          if (e.isNow) trans.eventInProgress() else momentFromNow(e.startsAt)
        )
      )
    )

  def spotlight(r: lidraughts.relay.Relay)(implicit ctx: Context) = {
    val splitName = r.name.split(" - ")
    val description = splitName.tail.lastOption.getOrElse(shorten(r.description, 30))
    a(
      href := routes.Relay.show(r.slug, r.id.value).url,
      cls := List(
        s"tour_spotlight event_spotlight id_${r.id}" -> true,
        "invert" -> r.isNowOrSoon
      )
    )(
        i(cls := "img", dataIcon := ""),
        span(cls := "content")(
          span(cls := "name")(splitName.head),
          span(cls := "headline")(description),
          span(cls := "more")(
            if (r.hasStarted) trans.eventInProgress() else r.startsAt.fold(emptyFrag)(momentFromNow(_))
          )
        )
      )
  }
}
