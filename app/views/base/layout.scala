package views.html.base

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import scalatags.Text.tags2.{ title => titleTag }

import controllers.routes

object layout {

  private val fontVersion = "04"

  private val doctype = raw("<!doctype html>")
  private val topComment = raw("""<!-- Lidraughts is open source, a fork of Lichess! See https://github.com/roepstoep/lidraughts -->""")
  private val charset = raw("""<meta charset="utf-8">""")
  private val fontStylesheets = raw(List(
    """<link href="https://fonts.googleapis.com/css?family=Noto+Sans:400,700|Roboto:300" rel="stylesheet">""",
    """<link href="https://fonts.googleapis.com/css?family=Roboto+Mono:500&text=0123456789:." rel="stylesheet">"""
  ).mkString)
  private val noTranslate = raw("""<meta name="google" content="notranslate" />""")
  private val fontPreload = raw(s"""<link rel="preload" href="${staticUrl(s"font$fontVersion/fonts/lidraughts.woff")}" as="font" type="font/woff" crossorigin/>""")
  private val manifests = raw(List(
    """<link rel="manifest" href="/manifest.json" />"""
  ).mkString)

  private val favicons = raw {
    List(256, 128, 64) map { px =>
      s"""<link rel="icon" type="image/png" href="${staticUrl(s"favicon.$px.png")}" sizes="${px}x${px}">"""
    } mkString ("", "", s"""<link id="favicon" rel="icon" type="image/png" href="${staticUrl("images/favicon-32-white.png")}" sizes="32x32">""")
  }
  private def blindModeForm(implicit ctx: Context) = raw(s"""<form id="blind_mode" action="${routes.Main.toggleBlindMode}" method="POST"><input type="hidden" name="enable" value="${if (ctx.blindMode) 0 else 1}" /><input type="hidden" name="redirect" value="${ctx.req.path}" /><button type="submit">Accessibility: ${if (ctx.blindMode) "Disable" else "Enable"} blind mode</button></form>""")
  private val zenToggle = raw("""<a data-icon="E" id="zentog" class="text fbt active">ZEN MODE</a>""")
  private def dasher(me: lidraughts.user.User) = raw(s"""<div class="dasher"><a id="user_tag" class="toggle link">${me.username}</a><div id="dasher_app" class="dropdown"></div></div>""")

  private def challenges(implicit ctx: Context) = spaceless(s"""<div class="challenge_notifications">
  <a id="challenge_notifications_tag" class="toggle link data-count" data-count="${ctx.nbChallenges}">
    <span class="hint--bottom-left" data-hint="${trans.challenges()}"><span data-icon="U"></span></span>
  </a>
  <div id="challenge_app" class="dropdown"></div>
</div>""")

  private def notifications(implicit ctx: Context) = spaceless(s"""<div class="site_notifications">
  <a id="site_notifications_tag" class="toggle link data-count" data-count="${ctx.nbNotifications}">
    <span class="hint--bottom-left" data-hint="${trans.notifications()}"><span data-icon=""></span></span>
  </a>
  <div id="notify_app" class="dropdown"></div>
</div>""")

  private def anonDasher(playing: Boolean)(implicit ctx: Context) = spaceless(s"""<div class="dasher">
  <a class="toggle anon">
    <span class="hint--bottom-left" data-hint="${trans.preferences()}"><span data-icon="%"></span></span>
  </a>
  <div id="dasher_app" class="dropdown" data-playing="$playing"></div>
</div>
<a href="${routes.Auth.login}?referrer=${currentPath}" class="signin button text">${trans.signIn()}</a>""")

  private val clinputLink = a(cls := "link")(span(dataIcon := "y"))

  private def clinput(implicit ctx: Context) =
    div(id := "clinput")(
      clinputLink,
      input(spellcheck := "false", placeholder := trans.search.txt())
    )

  private lazy val botImage = img(src := staticUrl("images/icons/bot.png"), title := "Robot draughts", style := "display:inline;width:34px;height:34px;vertical-align:top;margin-right:5px;")

  private val spaceRegex = """\s{2,}+""".r
  private def spaceless(html: String) = raw(spaceRegex.replaceAllIn(html.replace("\\n", ""), ""))

  private val dataDev = attr("data-dev")
  private val dataUser = attr("data-user")
  private val dataSoundSet = attr("data-sound-set")
  private val dataSocketDomain = attr("data-socket-domain")
  private val dataAssetUrl = attr("data-asset-url")
  private val dataAssetVersion = attr("data-asset-version")
  private val dataNonce = attr("data-nonce")
  private val dataZoom = attr("data-zoom")
  private val dataPreload = attr("data-preload")
  private val dataPlaying = attr("data-playing")
  private val dataPatrons = attr("data-patrons")
  private val dataStudying = attr("data-studying")

  def apply(
    title: String,
    fullTitle: Option[String] = None,
    baseline: Option[Html] = None,
    side: Option[Html] = None,
    menu: Option[Html] = None,
    chat: Option[Html] = None,
    underchat: Option[Html] = None,
    robots: Boolean = isGloballyCrawlable,
    moreCss: Html = emptyHtml,
    moreJs: Html = emptyHtml,
    playing: Boolean = false,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None,
    draughtsground: Boolean = true,
    zoomable: Boolean = false,
    asyncJs: Boolean = false,
    csp: Option[lidraughts.common.ContentSecurityPolicy] = None
  )(body: Html)(implicit ctx: Context) = frag(
    doctype,
    html(st.lang := ctx.lang.language)(
      topComment,
      head(
        charset,
        cspEnabled() option raw(
          s"""<meta http-equiv="Content-Security-Policy" content="${csp.getOrElse(defaultCsp)}">"""
        ),
        if (isProd) frag(
          titleTag(fullTitle | s"$title • lidraughts.org"),
          fontStylesheets
        )
        else frag(
          titleTag(s"[dev] ${fullTitle | s"$title • lidraughts.org"}"),
          cssAt("offline/font.noto.css"),
          cssAt("offline/font.roboto.mono.css")
        ),
        ctx.currentBg match {
          case "dark" => cssTag("dark.css")
          case "transp" => cssTags("dark.css", "transp.css")
          case _ => emptyHtml
        },
        cssTag("common.css"),
        cssTag("board.css"),
        ctx.zoom ifTrue zoomable map { z =>
          zoomStyle(z / 100f, false)
        },
        ctx.pref.coords == 1 option cssTag("board.coords.inner.css"),
        ctx.pageData.inquiry.isDefined option cssTag("inquiry.css"),
        ctx.userContext.impersonatedBy.isDefined option cssTag("impersonate.css"),
        isStage option cssTag("stage.css"),
        moreCss,
        link(id := "piece-sprite", href := assetUrl(s"stylesheets/piece/${ctx.currentPieceSet}.css"), `type` := "text/css", rel := "stylesheet"),
        meta(content := openGraph.fold(trans.siteDescription.txt())(o => o.description), name := "description"),
        favicons,
        !robots option raw("""<meta content="noindex, nofollow" name="robots">"""),
        noTranslate,
        openGraph.map(_.frag),
        link(href := routes.Blog.atom, `type` := "application/atom+xml", rel := "alternate", st.title := trans.blog.txt()),
        ctx.transpBgImg map { img =>
          raw(s"""<style type="text/css" id="bg-data">body.transp::before{background-image:url('$img');}</style>""")
        },
        fontPreload,
        manifests
      ),
      st.body(
        cls := List(
          "preload base" -> true,
          ctx.currentTheme.cssClass -> true,
          (if (ctx.currentBg == "transp") "dark transp" else ctx.currentBg) -> true,
          "zen" -> ctx.pref.isZen,
          "blind_mode" -> ctx.blindMode,
          "kid" -> ctx.kid,
          "mobile" -> ctx.isMobileBrowser,
          "playing fixed-scroll" -> playing
        ),
        dataDev := (!isProd).option("true"),
        dataUser := ctx.userId,
        dataSoundSet := ctx.currentSoundSet.toString,
        dataSocketDomain := socketDomain,
        dataAssetUrl := assetBaseUrl,
        dataAssetVersion := assetVersion.value,
        dataNonce := ctx.nonce.map(_.value),
        dataZoom := ctx.zoom.map(_.toString)
      )(
          blindModeForm,
          div(id := "site_description")(trans.siteDescription()),
          ctx.pageData.inquiry map { views.html.mod.inquiry(_) },
          ctx.me ifTrue ctx.userContext.impersonatedBy.isDefined map { views.html.mod.impersonate(_) },
          isStage option div(id := "stage")(
            "This is an empty lidraughts preview website for developers. ",
            a(href := "https://lidraughts.org")("Go to lidraughts.org instead")
          ),
          lidraughts.security.EmailConfirm.cookie.get(ctx.req).map(views.html.auth.emailConfirmBanner(_)),
          playing option zenToggle,
          div(id := "top", cls := "is2d")(
            topmenu(),
            div(id := "ham-plate", cls := "link hint--bottom", dataHint := trans.menu.txt())(
              div(id := "hamburger", dataIcon := "[")
            ),
            ctx.me map { me =>
              frag(dasher(me), challenges, notifications)
            } getOrElse {
              !ctx.pageData.error option anonDasher(playing)
            },
            ctx.teamNbRequests > 0 option
              a(cls := "link data-count", href := routes.Team.requests, dataCount := ctx.teamNbRequests)(
                span(cls := "hint--bottom-left", dataHint := trans.teams.txt())(span(dataIcon := "f"))
              ),
            isGranted(_.SeeReport) option
              a(cls := "link text data-count", href := routes.Report.list, dataCount := reportNbOpen, dataIcon := ""),
            clinput,
            a(id := "reconnecting", cls := "link text", dataIcon := "B")(trans.reconnecting())
          ),
          div(cls := "content is2d")(
            div(id := "site_header")(
              div(id := "notifications"),
              div(cls := "board_left")(
                h1(
                  a(id := "site_title", href := routes.Lobby.home)(
                    if (ctx.kid) span(st.title := trans.kidMode.txt(), cls := "kiddo")("😊")
                    else ctx.isBot option botImage,
                    "lidraughts",
                    span(cls := "extension")(if (isProd) ".org" else " dev")
                  )
                ),
                baseline,
                menu map { sideMenu =>
                  div(cls := "side_menu")(sideMenu)
                },
                side,
                chat
              ),
              underchat map { g =>
                div(cls := "under_chat")(g)
              }
            ),
            div(id := "lidraughts")(body)
          ),
          ctx.me.map { me =>
            div(
              id := "friend_box",
              dataPreload := ctx.onlineFriends.users.map(_.titleName).mkString(","),
              dataPlaying := ctx.onlineFriends.playing.mkString(","),
              dataPatrons := ctx.onlineFriends.patrons.mkString(","),
              dataStudying := ctx.onlineFriends.studying.mkString(",")
            )(
                div(cls := "friend_box_title")(
                  strong(cls := "online")(" "),
                  " ",
                  trans.onlineFriends()
                ),
                div(cls := "content_wrap")(
                  div(cls := "content list"),
                  div(cls := List(
                    "nobody" -> true,
                    "none" -> ctx.onlineFriends.users.nonEmpty
                  ))(
                    span(trans.noFriendsOnline()),
                    a(cls := "find button", href := routes.User.opponents)(
                      span(cls := "is3 text", dataIcon := "h")(trans.findFriends())
                    )
                  )
                )
              )
          },
          draughtsground option jsTag("vendor/draughtsground.min.js"),
          ctx.requiresFingerprint option fingerprintTag,
          jsAt(s"compiled/lidraughts.site${isProd ?? ".min"}.js", async = asyncJs),
          moreJs,
          embedJs(s"""lidraughts.quantity=${lidraughts.i18n.JsQuantity(ctx.lang)};$timeagoLocaleScript"""),
          ctx.pageData.inquiry.isDefined option jsTag("inquiry.js", async = asyncJs)
        )
    )
  )
}
