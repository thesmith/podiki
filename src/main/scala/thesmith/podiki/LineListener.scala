package thesmith.podiki

import com.top10.redis.Redis
import thesmith.podiki.podcast.Episode
import org.slf4j.LoggerFactory

class LineListener(redis: Redis) {
  val logger = LoggerFactory.getLogger(this.getClass)

  def line(episode: Episode, line: String) {
    logger.info("Adding line: "+line+" to: "+episode)
    redis.lpush("episode_lines:"+episode.url, line)
  }
  
}