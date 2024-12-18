package lidraughts.app
package templating

import java.util.Locale
import scala.collection.mutable.AnyRefMap

import org.joda.time.format._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ Period, PeriodType, DurationFieldType, DateTime, DateTimeZone }

import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.Lang

trait DateHelper { self: I18nHelper =>

  private val dateTimeStyle = "MS"
  private val dateStyle = "M-"

  private val dateTimeFormatters = AnyRefMap.empty[String, DateTimeFormatter]
  private val dateFormatters = AnyRefMap.empty[String, DateTimeFormatter]
  private val periodFormatters = AnyRefMap.empty[String, PeriodFormatter]
  private val periodType = PeriodType forFields Array(
    DurationFieldType.days,
    DurationFieldType.hours,
    DurationFieldType.minutes
  )

  private val isoFormatter = ISODateTimeFormat.dateTime

  private val englishDateFormatter = DateTimeFormat forStyle dateStyle

  private def dateTimeFormatter(implicit lang: Lang): DateTimeFormatter =
    dateTimeFormatters.getOrElseUpdate(
      lang.code,
      DateTimeFormat forStyle dateTimeStyle withLocale lang.toLocale
    )

  private def dateFormatter(implicit lang: Lang): DateTimeFormatter =
    dateFormatters.getOrElseUpdate(
      lang.code,
      DateTimeFormat forStyle dateStyle withLocale lang.toLocale
    )

  private def periodFormatter(implicit lang: Lang): PeriodFormatter =
    periodFormatters.getOrElseUpdate(
      lang.code, {
        Locale setDefault Locale.ENGLISH
        PeriodFormat wordBased lang.toLocale
      }
    )

  def showDateTimeZone(date: DateTime, zone: DateTimeZone)(implicit lang: Lang): String =
    dateTimeFormatter print date.toDateTime(zone)

  def showDateTimeUTC(date: DateTime)(implicit lang: Lang): String =
    showDateTimeZone(date, DateTimeZone.UTC)

  def showDate(date: DateTime)(implicit lang: Lang): String =
    dateFormatter print date

  def showEnglishDate(date: DateTime): String =
    englishDateFormatter print date

  def semanticDate(date: DateTime)(implicit lang: Lang): Frag =
    timeTag(datetimeAttr := isoDate(date))(showDate(date))

  def showPeriod(period: Period)(implicit lang: Lang): String =
    periodFormatter print period.normalizedStandard(periodType)

  def showMinutes(minutes: Int)(implicit lang: Lang): String =
    showPeriod(new Period(minutes * 60 * 1000l))

  def isoDate(date: DateTime): String = isoFormatter print date

  private val oneDayMillis = 1000 * 60 * 60 * 24

  def momentFromNow(date: DateTime, alwaysRelative: Boolean = false, once: Boolean = false): Frag = {
    if (!alwaysRelative && (date.getMillis - nowMillis) > oneDayMillis) absClientDateTime(date)
    else timeTag(cls := s"timeago${once ?? " once"}", datetimeAttr := isoDate(date))
  }

  def absClientDateTime(date: DateTime): Frag =
    timeTag(cls := "timeago abs", datetimeAttr := isoDate(date))("-")

  def momentFromNowOnce(date: DateTime) = momentFromNow(date, once = true)

  def secondsFromNow(seconds: Int, alwaysRelative: Boolean = false)(implicit lang: Lang) =
    momentFromNow(DateTime.now plusSeconds seconds, alwaysRelative)

  private val atomDateFormatter = ISODateTimeFormat.dateTime
  def atomDate(date: DateTime): String = atomDateFormatter print date
  def atomDate(field: String)(doc: io.prismic.Document): Option[String] =
    doc getDate field map (_.value.toDateTimeAtStartOfDay) map atomDate
}
