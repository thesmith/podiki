package thesmith.podiki

import org.slf4j.LoggerFactory
import thesmith.podiki.echoprint.EchoPrint
import scala.sys.process._
import thesmith.podiki.http.UrlFetcher
import thesmith.podiki.podcast.Podcast
import com.top10.redis.SingleRedis
import java.io.File
import java.io.FileOutputStream

object App extends scala.App {
  val logger = LoggerFactory.getLogger("Podiki")
  val urlFetcher = new UrlFetcher(30, 30, 30)
  val redis = new SingleRedis("localhost", 6379, Some("onerecruit"))
  val echoPrint = new EchoPrint("/Users/bens/projects/echoprint-codegen/echoprint-codegen", urlFetcher)
  val podcast = new Podcast("/tmp", urlFetcher)
  redis.zadd("podcast_urls", 1, "http://marenco.libsyn.com/rss")
  
  val podcastCrawler = new PodcastCrawler(redis, podcast, echoPrint)
  podcastCrawler.start
  
  this.synchronized(this.wait)
  
  podcastCrawler.shutdown
}