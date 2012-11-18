package thesmith.podiki.s2t

import edu.cmu.sphinx.util.props.ConfigurationManagerUtils
import edu.cmu.sphinx.result.Result
import scala.annotation.tailrec
import edu.cmu.sphinx.recognizer.Recognizer
import javax.sound.sampled.AudioSystem
import java.net.URL
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import java.io.File
import javax.sound.sampled.AudioFormat.Encoding
import javax.sound.sampled.AudioFileFormat
import org.slf4j.LoggerFactory
import javazoom.jl.converter.Converter

class SpeachToText {
  val logger = LoggerFactory.getLogger(this.getClass)
  val targetFormat = new AudioFormat(16000f, 16, 1, true, true);
    val converter = new Converter()
  
  def toText(sourcePath: String): Seq[String] = {
    val config = new SphinxConf()
    val tempPath = "/tmp/"+System.currentTimeMillis+".wav"
    converter.convert(sourcePath, tempPath)
    logger.info("Converted "+sourcePath+" to "+tempPath)
    
    val recognizer = config.recognizer
    recognizer.allocate()
    config.audioDataSource.setAudioFile(new URL("file:"+tempPath), null)
    
    @tailrec def recog(lines: Seq[String]): Seq[String] = Option(recognizer.recognize) match {
      case Some(result) => {
        val line = result.getBestFinalResultNoFiller
        println("+++++ "+line+" - "+result)
        recog(lines :+ line)
      }
      case _ => lines
    }
    
    recog(Seq())
  }
  
  def convert(sourceAis: AudioInputStream, encoding: Encoding, sampleRate: Float, channels: Int): AudioInputStream = {
    val baseFormat = sourceAis.getFormat()
    println("Converting to encoding: "+encoding+", sample rate: "+sampleRate+", channels: "+channels+", from: "+baseFormat)
    val intermediateFormat = new AudioFormat(
          encoding, sampleRate, baseFormat.getSampleSizeInBits(), channels,
          channels * 2, sampleRate, false)
    AudioSystem.getAudioInputStream(intermediateFormat, sourceAis)
  }
  
  def convertAudioInputStream(sourceAis: AudioInputStream, targetFormat: AudioFormat): AudioInputStream = {
    val baseFormat = sourceAis.getFormat()
    
    if (!baseFormat.getEncoding().equals(targetFormat.getEncoding())) {
      convertAudioInputStream(convert(sourceAis, targetFormat.getEncoding, baseFormat.getSampleRate, baseFormat.getChannels), targetFormat)
//    } else if (baseFormat.getSampleRate() != targetFormat.getSampleRate()) {
//      convertAudioInputStream(convert(sourceAis, targetFormat.getEncoding, targetFormat.getSampleRate, baseFormat.getChannels), targetFormat)
    } else if (baseFormat.getChannels() > targetFormat.getChannels()) {
      convertAudioInputStream(convert(sourceAis, targetFormat.getEncoding, targetFormat.getSampleRate, targetFormat.getChannels), targetFormat)
    } else {
      sourceAis
    }
  }
  
  def needToConvert(sourceFormat: AudioFormat, targetFormat: AudioFormat): Boolean = {
    (! sourceFormat.getEncoding().equals(targetFormat.getEncoding()) || 
       sourceFormat.getSampleRate() != targetFormat.getSampleRate() || 
       sourceFormat.getChannels() > targetFormat.getChannels())
  }
  
  def writeConvertedFile(sourceAis: AudioInputStream, fileName: String): File = {
    val path = fileName.substring(5, fileName.length()-4) + "_new.wav"
    logger.info("Writing audio from "+fileName+" to "+path)
    val file = new File(path)
    AudioSystem.write(sourceAis, AudioFileFormat.Type.WAVE, file)
    file
  }
}