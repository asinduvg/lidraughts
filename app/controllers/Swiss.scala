package controllers

import play.api.libs.json._
import play.api.mvc._

import lidraughts.api.Context
import lidraughts.app._
import lidraughts.chat.Chat
import lidraughts.swiss.{ Swiss => SwissModel }
import lidraughts.swiss.Swiss.{ Id => SwissId }
import views._

object Swiss extends LidraughtsController {

  private def env = Env.swiss

  private def swissNotFound(implicit ctx: Context) = NotFound(html.swiss.bits.notFound())

  def show(id: String) = Secure(_.Beta) { implicit ctx => _ =>
    env.api.byId(SwissId(id)) flatMap { swissOption =>
      val page = getInt("page").filter(0.<)
      negotiate(
        html = swissOption.fold(swissNotFound.fuccess) { swiss =>
          for {
            version <- env.version(swiss.id)
            isInTeam <- isCtxInTheTeam(swiss.teamId)
            json <- env.json(
              swiss = swiss,
              me = ctx.me,
              reqPage = page,
              socketVersion = version.some,
              playerInfo = none,
              isInTeam = isInTeam
            )
            canChat <- canHaveChat(swiss)
            chat <- canChat ?? Env.chat.api.userChat.cached
              .findMine(lidraughts.chat.Chat.Id(swiss.id.value), ctx.me)
              .dmap(some)
            _ <- chat ?? { c =>
              Env.user.lightUserApi.preloadMany(c.chat.userIds)
            }
          } yield Ok(html.swiss.show(swiss, json, chat))
        },
        api = _ =>
          swissOption.fold(notFoundJson("No such swiss tournament")) { swiss =>
            for {
              socketVersion <- getBool("socketVersion").??(env version swiss.id dmap some)
              isInTeam <- isCtxInTheTeam(swiss.teamId)
              playerInfo <- get("playerInfo").?? { env.api.playerInfo(swiss, _) }
              json <- env.json(
                swiss = swiss,
                me = ctx.me,
                reqPage = page,
                socketVersion = socketVersion,
                playerInfo = playerInfo,
                isInTeam = isInTeam
              )
            } yield Ok(json)
          }
      )
    }
  }

  private def isCtxInTheTeam(teamId: lidraughts.team.Team.ID)(implicit ctx: Context) =
    ctx.userId.??(u => Env.team.cached.teamIds(u).dmap(_ contains teamId))

  def form(teamId: String) = Secure(_.Beta) { implicit ctx => me =>
    Ok(html.swiss.form.create(env.forms.create, teamId)).fuccess
  }

  def create(teamId: String) = SecureBody(_.Beta) { implicit ctx => me =>
    env.forms.create
      .bindFromRequest()(ctx.body)
      .fold(
        err => BadRequest(html.swiss.form.create(err, teamId)).fuccess,
        data =>
          Tournament.rateLimitCreation(me, false, ctx.req) {
            env.api.create(data, me, teamId) map { swiss =>
              Redirect(routes.Swiss.show(swiss.id.value))
            }
          }
      )
  }

  def websocket(id: String, apiVersion: Int) = SocketOption[JsValue] { implicit ctx =>
    getSocketUid("sri") ?? { uid =>
      env.socketHandler.join(id, uid, ctx.me, getSocketVersion, apiVersion)
    }
  }

  def join(id: String) = SecureBody(_.Beta) { implicit ctx => me =>
    NoLameOrBot {
      Env.team.cached.teamIds(me.id) flatMap { teamIds =>
        env.api.joinWithResult(SwissId(id), me, teamIds.contains) flatMap { result =>
          negotiate(
            html = Redirect(routes.Swiss.show(id)).fuccess,
            api = _ =>
              fuccess {
                if (result) jsonOkResult
                else BadRequest(Json.obj("joined" -> false))
              }
          )
        }
      }
    }
  }

  def withdraw(id: String) =
    Auth { implicit ctx => me =>
      WithSwiss(id) { swiss =>
        env.api.withdraw(SwissId(id), me)
        if (lidraughts.common.HTTPRequest.isXhr(ctx.req)) fuccess(jsonOkResult)
        else Redirect(routes.Swiss.show(id)).fuccess
      }
    }

  def edit(id: String) = Auth { implicit ctx => me =>
    WithEditableSwiss(id, me) { swiss =>
      Ok(html.swiss.form.edit(swiss, env.forms.edit(swiss))).fuccess
    }
  }

  def update(id: String) = AuthBody { implicit ctx => me =>
    WithEditableSwiss(id, me) { swiss =>
      implicit val req = ctx.body
      env.forms.edit(swiss)
        .bindFromRequest
        .fold(
          err => BadRequest(html.swiss.form.edit(swiss, err)).fuccess,
          data => env.api.update(swiss, data) inject Redirect(routes.Swiss.show(id))
        )
    }

  }
  def terminate(id: String) = Auth { implicit ctx => me =>
    WithEditableSwiss(id, me) { swiss =>
      env.api kill swiss
      Redirect(routes.Team.show(swiss.teamId)).fuccess
    }
  }

  def standing(id: String, page: Int) = Open { implicit ctx =>
    WithSwiss(id) { swiss =>
      JsonOk {
        env.standingApi(swiss, page)
      }
    }
  }

  def player(id: String, userId: String) = Action.async {
    WithSwiss(id) { swiss =>
      env.api.playerInfo(swiss, userId) flatMap {
        _.fold(notFoundJson()) { player =>
          JsonOk(fuccess(lidraughts.swiss.SwissJson.playerJsonExt(swiss, player)))
        }
      }
    }
  }

  private def WithSwiss(id: String)(f: SwissModel => Fu[Result]): Fu[Result] =
    env.api.byId(SwissId(id)) flatMap { _ ?? f }

  private def WithEditableSwiss(id: String, me: lidraughts.user.User)(
    f: SwissModel => Fu[Result]
  )(implicit ctx: Context): Fu[Result] =
    WithSwiss(id) { swiss =>
      if (swiss.createdBy == me.id && !swiss.isFinished) f(swiss)
      else if (isGranted(_.ManageTournament)) f(swiss)
      else Redirect(routes.Swiss.show(swiss.id.value)).fuccess
    }

  private def canHaveChat(swiss: SwissModel)(implicit ctx: Context): Fu[Boolean] =
    (swiss.settings.hasChat && ctx.noKid) ?? ctx.userId.?? {
      Env.team.api.belongsTo(swiss.teamId, _)
    }
}