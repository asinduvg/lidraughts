package lidraughts.streamer

import play.api.libs.json._
import org.joda.time.DateTime

import lidraughts.user.User
import lidraughts.common.String.html.unescapeHtml

trait Stream {
  def serviceName: String
  val status: String
  val streamer: Streamer
  def is(s: Streamer): Boolean = streamer.id == s.id
  def is(userId: User.ID): Boolean = streamer.userId == userId
  def twitch = serviceName == "twitch"
  def youTube = serviceName == "youTube"
}

object Stream {

  case class Keyword(value: String) extends AnyRef with StringValue {
    def toLowerCase = value.toLowerCase
  }

  object Twitch {
    case class TwitchStream(user_name: String, title: String, `type`: String) {
      def name = user_name
      def isLive = `type` == "live"
    }
    case class Result(data: Option[List[TwitchStream]]) {
      def liveStreams = (~data).filter(_.isLive)
      def streams(keyword: Keyword, streamers: List[Streamer], alwaysFeatured: List[User.ID]): List[Stream] = liveStreams.collect {
        case TwitchStream(name, title, _) =>
          streamers.filter { s =>
            s.twitch.exists(_.userId.toLowerCase == name.toLowerCase) && {
              title.toLowerCase.contains(keyword.toLowerCase) ||
                alwaysFeatured.contains(s.userId)
            }
          } map { Stream(name, title, _) }
      }.flatten
    }
    case class Stream(userId: String, status: String, streamer: Streamer) extends lidraughts.streamer.Stream {
      def serviceName = "twitch"
    }
    object Reads {
      private implicit val twitchStreamReads = Json.reads[TwitchStream]
      implicit val twitchResultReads = Json.reads[Result]
    }
  }

  object YouTube {
    case class Snippet(channelId: String, title: String, liveBroadcastContent: String)
    case class Id(videoId: String)
    case class Item(id: Id, snippet: Snippet)
    case class Result(items: List[Item]) {
      def streams(keyword: Keyword, streamers: List[Streamer]): List[Stream] =
        items.filter { item =>
          item.snippet.liveBroadcastContent == "live" &&
            item.snippet.title.toLowerCase.contains(keyword.toLowerCase)
        }.flatMap { item =>
          streamers.find(s => s.youTube.exists(_.channelId == item.snippet.channelId)) map {
            Stream(
              item.snippet.channelId,
              unescapeHtml(item.snippet.title),
              item.id.videoId,
              _
            )
          }
        }
    }
    case class Stream(channelId: String, status: String, videoId: String, streamer: Streamer) extends lidraughts.streamer.Stream {
      def serviceName = "youTube"
    }

    object Reads {
      private implicit val youtubeSnippetReads = Json.reads[Snippet]
      private implicit val youtubeIdReads = Json.reads[Id]
      private implicit val youtubeItemReads = Json.reads[Item]
      implicit val youtubeResultReads = Json.reads[Result]
    }

    case class StreamsFetched(list: List[YouTube.Stream], at: DateTime)
  }
}
