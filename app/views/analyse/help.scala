package views.html.analyse

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object help {

  private def header(text: String) = tr(
    th(colspan := 2)(
      p(text)
    )
  )
  private def row(keys: Frag, desc: String) = tr(
    td(cls := "keys")(keys),
    td(cls := "desc")(desc)
  )
  private val or = raw("""<or>/</or>""")
  private val to = raw("""<or>-</or>""")
  private def k(str: String) = raw(s"""<kbd>$str</kbd>""")

  def apply(isStudy: Boolean)(implicit ctx: Context) = frag(
    h2(trans.keyboardShortcuts()),
    table(
      tbody(
        header("Navigate the move tree"),
        row(frag(k("←"), or, k("→")), trans.keyMoveBackwardOrForward.txt()),
        row(frag(k("j"), or, k("k")), trans.keyMoveBackwardOrForward.txt()),
        row(frag(k("↑"), or, k("↓")), trans.keyGoToStartOrEnd.txt()),
        row(frag(k("0"), or, k("$")), trans.keyGoToStartOrEnd.txt()),
        row(frag(k("shift"), k("←"), or, k("shift"), k("→")), trans.keyEnterOrExitVariation.txt()),
        row(frag(k("shift"), k("J"), or, k("shift"), k("K")), trans.keyEnterOrExitVariation.txt()),
        row(frag(k("ctrl"), k("1"), to, k("9")), "Bookmark position"),
        row(frag(k("1"), to, k("9")), "Jump to bookmarked position"),
        row(frag(k("o")), "Jump to current game position"),
        header("Analysis options"),
        row(frag(k("shift"), k("I")), trans.inlineNotation.txt()),
        row(frag(k("l")), "Local computer analysis"),
        row(frag(k("z")), "Toggle all computer analysis"),
        row(frag(k("a")), "Computer arrows"),
        row(frag(k("space")), "Play computer best move"),
        row(frag(k("x")), "Show threat"),
        // row(frag(k("e")), "Opening/endgame explorer"),
        row(frag(k("f")), trans.flipBoard.txt()),
        row(frag(k("c")), "Focus chat"),
        row(frag(k("shift"), k("C")), trans.keyShowOrHideComments.txt()),
        row(frag(k("?")), "Show this help dialog"),
        isStudy option frag(
          header("Study actions"),
          row(frag(k("c")), "Comment this position"),
          row(frag(k("g")), "Annotate with glyphs")
        ),
        header("Mouse tricks"),
        tr(
          td(cls := "mouse", colspan := 2)(
            ul(
              li(trans.youCanAlsoScrollOverTheBoardToMoveInTheGame()),
              li(trans.analysisShapesHowTo())
            )
          )
        )
      )
    )
  )
}
