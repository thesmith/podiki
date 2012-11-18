package thesmith.podiki.podcast

import thesmith.podiki.http.UrlFetcher
import scala.xml.XML
import java.net.URL
import java.io.File
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.io.FileOutputStream
import org.apache.commons.io.IOUtils
import java.net.URLEncoder

class Podcast(repo: String, urlFetcher: UrlFetcher) {
  val logger = LoggerFactory.getLogger(this.getClass)
  val fmt = DateTimeFormat.forPattern("E, d MMM y HH:mm:ss Z")

  def mp3s(url: String): Seq[Episode] = {
    val mp3s = urlFetcher.get(url).content.map(content => {
      val xml = scala.xml.parsing.XhtmlParser(scala.io.Source.fromString(content))
      xml \\ "item" map(item => {
        val name = (item \ "title").text
        val url = (item \ "link").text
        val thumbnail = (item \ "media:thumbnail" \ "@url").text
        val mp3Url = (item \ "enclosure" \ "@url").text
        val published = fmt.parseDateTime((item \ "pubDate").text)
        Episode(name, published, url, "", mp3Url, thumbnail)
      })
    })
    logger.info(mp3s.mkString(", "))
    mp3s.getOrElse(Seq()).flatMap(episode => {
      try {
        if (! episode.mp3Url.isEmpty) {
          val u = new URL(episode.mp3Url)
          val path = u.getFile.replace("/", "_")
          val file = new File(repo+"/"+path)
          if (!file.exists) {
            logger.info("Getting mp3: "+episode.mp3Url+" and putting it: "+repo+"/"+path)
            val response = urlFetcher.get(episode.mp3Url, Map(), false)
            response.contentStream.map(contentStream => {
              inputToFile(contentStream, file)
              logger.info("Written: "+file.getAbsolutePath)
            })
          }
          Some(episode.withMp3Path(file.getAbsolutePath))
        } else None
      } catch {
        case e => {
          logger.error("Problem processing episode: "+episode, e)
          e.printStackTrace
          None
        }
      }
    })
  }
  
  protected def inputToFile(is: java.io.InputStream, f: java.io.File) {
    val os = new FileOutputStream(f.getAbsolutePath)
    IOUtils.copy(is, os)
    os.close()
  }
  
}

case class Episode(name: String, published: DateTime, url: String, mp3Path: String, mp3Url: String, thumbnail: String) {
  def withMp3Path(mp3Path: String) = Episode(name, published, url, mp3Path, mp3Url, thumbnail)
  
  def toMap = {
    Map("name" -> name,
        "url" -> url,
        "mp3_path" -> mp3Path,
        "mp3_url" -> mp3Url,
        "thumbnail" -> thumbnail,
        "published" -> published.getMillis.toString,
        "id" -> URLEncoder.encode(url, "UTF-8"))
  }
}