package thesmith.podiki

import scala.util.Random
import com.top10.redis.Redis
import thesmith.podiki.podcast.Podcast
import thesmith.podiki.echoprint.EchoPrint
import org.slf4j.LoggerFactory
import thesmith.podiki.s2t.SpeachToText
import scala.actors.Futures._

class PodcastCrawler(redis: Redis, podcast: Podcast, echoPrint: EchoPrint, spearchToText: SpeachToText) extends Thread {
  val batchSize = 10
  val key = "podcast_urls"
  val logger = LoggerFactory.getLogger(this.getClass)
  
  var running = true

  override def run() {
    var currentIndex = -1 
    while (running) {
      if (currentIndex == -1) {
        currentIndex = Random.nextInt(redis.zcard(key).toInt)
      }
  
      val batch = redis.zrange(key, currentIndex, (currentIndex + batchSize) - 1)
      logger.info("Retrieved "+batch.size+" podcasts to process")
      if (batch.size < batchSize) {
        currentIndex = 0
      } else {
        currentIndex += batchSize
      }
      batch.foreach(url => {
        logger.info("Processing: "+url)
        val episodes = podcast.mp3s(url)
        logger.info(url+" returned "+episodes.size+" episodes")
        
        redis.exec(pipeline => {
          episodes.foreach(episode => {
            pipeline.zadd("podcast_episodes:"+url, episode.published.getMillis, episode.url)
            pipeline.hmset("episode:"+episode.url, episode.toMap)
          })
        })
        
        episodes.foreach(episode => {
          val tracksF = future { echoPrint.tracks(episode) }
          val linesF = future { spearchToText.toText(episode) }
          val tracks = tracksF()
          val lines = linesF()
          
          logger.info(episode+" returned "+tracks.size+" tracks and "+lines.size+" lines")
        })
      })
    }
  }
  
  def shutdown() {
    running = false
  }
}