package thesmith.podiki.echoprint

import org.apache.http.params.HttpConnectionParams
import org.apache.http.params.HttpParams
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.apache.http.params.BasicHttpParams
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.PlainSocketFactory
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.conn.ssl.SSLSocketFactory
import javax.net.ssl.HttpsURLConnection
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import java.io.IOException
import java.io.BufferedReader
import java.io.InputStreamReader
import scala.actors.Futures._
import net.liftweb.json.JsonParser
import net.liftweb.json.JsonAST.JValue
import org.slf4j.LoggerFactory
import org.tritonus.share.sampled.file.TAudioFileFormat
import javax.sound.sampled.AudioSystem
import java.io.File
import scala.sys.process._
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import java.net.URLEncoder
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Extraction.decompose
import net.liftweb.json.JsonAST.render
import net.liftweb.json.Printer.compact
import thesmith.podiki.http.UrlFetcher

class EchoPrint(location: String, urlFetcher: UrlFetcher) {
  val logger = LoggerFactory.getLogger(this.getClass)
  implicit val formats = net.liftweb.json.DefaultFormats
  val url = "http://developer.echonest.com/api/v4/song/identify?api_key=AFB4HZSDSRBTJGC5Q"
  
  def tracks(path: String): Seq[Song] = {
     val tasks = segments(length(path)).map(segment => {
      future {
        try {
          val cmd = location+" "+path+" "+segment._1+" "+segment._2
          logger.info(cmd)
          val result = cmd.!!
          val json = JsonParser.parse(result)
          Option(compact(render(decompose((json.extract[List[JValue]].head))))).flatMap(code => {
            JsonParser.parse(code).\("code").extractOpt[String].flatMap(c=> {
              val response = urlFetcher.post(url, "query="+URLEncoder.encode(code, "UTF-8"), Map("Content-Type" -> "application/x-www-form-urlencoded"))
              response.content
            })
          })
        } catch {
          case e => {
            e.printStackTrace
            None
          }
        }
      }
    })
    
    val results = awaitAll(1000000L, tasks: _*).flatten
    logger.info(results.mkString("\n"))
    results.map(_.asInstanceOf[Option[String]]).flatten.map(result => {
      val json = JsonParser.parse(result)
      (json \ "response" \ "songs").extractOpt[List[JValue]].getOrElse(List()).headOption.map(song => {
        val artist = (song \ "artist_name").extract[String]
        val track = (song \ "title").extract[String]
        Song(artist, track, 0)
      })
    }).flatten.distinct.zipWithIndex.map(r => r._1.withPosition(r._2))
  }
  
  def segments(length: Int): Seq[(Int, Int)] = {
    (10 to length by 120).map(i => (i -> (i + 10)))
  }
  
  def length(path: String): Int = {
    val fileFormat = AudioSystem.getAudioFileFormat(new File(path))
    val properties = fileFormat.asInstanceOf[TAudioFileFormat].properties()
    val microseconds = properties.get("duration").asInstanceOf[Long]
    val mili = (microseconds / 1000).toInt
    mili / 1000
  }
}

case class Song(artist: String, track: String, position: Int) {
  def withPosition(position: Int) = Song(artist, track, position)
}