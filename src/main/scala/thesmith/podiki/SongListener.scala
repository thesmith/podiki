package thesmith.podiki

import com.top10.redis.Redis
import thesmith.podiki.echoprint.Song
import thesmith.podiki.podcast.Episode
import org.slf4j.LoggerFactory
import thesmith.podiki.spotify.SpotifySearcher

class SongListener(redis: Redis, spotifySearcher: SpotifySearcher) {
  val logger = LoggerFactory.getLogger(this.getClass)

  def song(episode: Episode, song: Song) {
    logger.info("Adding song: "+song+" to episode: "+episode)
    redis.zadd("episode_tracks:"+episode.url, song.position, song.artist+" - "+song.track)
    spotifySearcher.search(episode, song)
  }
  
}