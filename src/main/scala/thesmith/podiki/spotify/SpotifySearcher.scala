package thesmith.podiki.spotify

import thesmith.podiki.http.UrlFetcher
import thesmith.podiki.echoprint.Song
import com.top10.redis.Redis
import net.liftweb.json.JsonParser
import thesmith.podiki.podcast.Episode
import net.liftweb.json.JsonAST.JValue
import java.net.URLEncoder

class SpotifySearcher(urlFetcher: UrlFetcher, redis: Redis) {
  implicit val formats = net.liftweb.json.DefaultFormats

  def search(episode: Episode, song: Song) {
    val response = urlFetcher.get("http://ws.spotify.com/search/1/track.json?q="+URLEncoder.encode(song.artist+", "+song.track, "UTF-8"))
    response.statusCode match {
      case 200 => response.content match {
        case Some(content) => {
          val json = JsonParser.parse(content)
          
          try {
            (json \ "tracks").extract[List[JValue]].headOption.foreach(track => {
              val link = (track \ "href").extract[String].replace("spotify:track:", "http://open.spotify.com/track/")
              redis.set("song_link:"+song.artist+" - "+song.track, link)
            })
          } catch {
            case e: Exception => {
              e.printStackTrace()
            }
          }
        }
      }
    }
  }
  
}