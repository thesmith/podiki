package thesmith.podiki.s2t

import edu.cmu.sphinx.decoder.Decoder
import edu.cmu.sphinx.decoder.ResultListener
import edu.cmu.sphinx.decoder.pruner.Pruner
import edu.cmu.sphinx.decoder.pruner.SimplePruner
import edu.cmu.sphinx.decoder.scorer.AcousticScorer
import edu.cmu.sphinx.decoder.scorer.ThreadedAcousticScorer
import edu.cmu.sphinx.decoder.search.ActiveListFactory
import edu.cmu.sphinx.decoder.search.PartitionActiveListFactory
import edu.cmu.sphinx.decoder.search.SearchManager
import edu.cmu.sphinx.decoder.search.SimpleBreadthFirstSearchManager
import edu.cmu.sphinx.frontend.DataBlocker
import edu.cmu.sphinx.frontend.DataProcessor
import edu.cmu.sphinx.frontend.FrontEnd
import edu.cmu.sphinx.frontend.endpoint.NonSpeechDataFilter
import edu.cmu.sphinx.frontend.endpoint.SpeechClassifier
import edu.cmu.sphinx.frontend.endpoint.SpeechMarker
import edu.cmu.sphinx.frontend.feature.DeltasFeatureExtractor
import edu.cmu.sphinx.frontend.feature.LiveCMN
import edu.cmu.sphinx.frontend.filter.Preemphasizer
import edu.cmu.sphinx.frontend.frequencywarp.MelFrequencyFilterBank
import edu.cmu.sphinx.frontend.transform.DiscreteCosineTransform
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform
import edu.cmu.sphinx.frontend.util.AudioFileDataSource
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower
import edu.cmu.sphinx.instrumentation.BestPathAccuracyTracker
import edu.cmu.sphinx.instrumentation.MemoryTracker
import edu.cmu.sphinx.instrumentation.Monitor
import edu.cmu.sphinx.instrumentation.SpeedTracker
import edu.cmu.sphinx.linguist.Linguist
import edu.cmu.sphinx.linguist.acoustic.AcousticModel
import edu.cmu.sphinx.linguist.acoustic.UnitManager
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Loader
import edu.cmu.sphinx.linguist.dictionary.Dictionary
import edu.cmu.sphinx.linguist.language.grammar.Grammar
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel
import edu.cmu.sphinx.recognizer.Recognizer
import edu.cmu.sphinx.util.LogMath
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.List
import java.util.logging.Level
import java.util.logging.Logger
import scala.collection.JavaConversions._
import edu.cmu.sphinx.jsgf.JSGFGrammar
import edu.cmu.sphinx.linguist.flat.FlatLinguist
import edu.cmu.sphinx.linguist.acoustic.tiedstate.Sphinx3Loader
import edu.cmu.sphinx.linguist.acoustic.tiedstate.TiedStateAcousticModel
import edu.cmu.sphinx.linguist.dictionary.FastDictionary
import java.net.URL
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils
import edu.cmu.sphinx.linguist.language.ngram.large.LargeNGramModel
import edu.cmu.sphinx.linguist.language.ngram.BackoffLanguageModel
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist
import edu.cmu.sphinx.linguist.language.ngram.SimpleNGramModel

class SphinxConf {
  val logMath = new LogMath(1.0001f, true)
  
  val absoluteBeamWidth = -1
  val relativeBeamWidth = 1E-80
  val wordInsertionProbability = 1E-36
  val languageWeight = 8.0f

  val audioDataSource = new AudioFileDataSource(3200, null)
  val speechMarker = new SpeechMarker(
    200, // startSpeechTime
    500, // endSilenceTime
    100, // speechLeader
    50,  // speechLeaderFrames
    100, // speechTrailer
    15.0 // endSilenceDecay
  )
  val dataBlocker = new DataBlocker(10)
  val speechClassifier = new SpeechClassifier(
    10,     // frameLengthMs
    0.003,  // adjustment
    10,     // threshold
    0       // minSignal
  )
  val nonSpeechDataFilter = new NonSpeechDataFilter()
  val premphasizer = new Preemphasizer(
    0.97 // preemphasisFactor
  )
  val windower = new RaisedCosineWindower(
    0.46, // double alpha
    25.625f, // windowSizeInMs
    10.0f // windowShiftInMs
  )
  
  val fft = new DiscreteFourierTransform(
    -1, // numberFftPoints
    false // invert
  )
  val melFilterBank = new MelFrequencyFilterBank(
    130.0, // minFreq,
    6800.0, // maxFreq,
    40 // numberFilters
  )
  val dct = new DiscreteCosineTransform(
    40, // numberMelFilters,
    13  // cepstrumSize
  )
  val cmn = new LiveCMN(
    12.0, // initialMean,
    100,  // cmnWindow,
    160   // cmnShiftWindow
  )
  val featureExtraction = new DeltasFeatureExtractor(
    3 // window
  )
  
  val pipeline = Seq[DataProcessor](audioDataSource, dataBlocker, speechClassifier, speechMarker,
                                    nonSpeechDataFilter, premphasizer, windower, fft, melFilterBank,
                                    dct, cmn, featureExtraction)

  val frontend = new FrontEnd(pipeline)

  val scorer = new ThreadedAcousticScorer(frontend, null, 10, true, 0, Thread.NORM_PRIORITY)

  val pruner = new SimplePruner()

  val activeListFactory = new PartitionActiveListFactory(absoluteBeamWidth, relativeBeamWidth, logMath)
  
  val unitManager = new UnitManager()

//  val modelLoader = new Sphinx3Loader("resource:/edu/cmu/sphinx/model/acoustic/HUB4_8gau_13dCep_16k_40mel_133Hz_6855Hz",
//                                      "hub4opensrc.6000.mdef", "6000senones/hub4opensrc.cd_continuous_8gau/", logMath, unitManager, 0.0f, 1e-7f, 0.0001f, true)
  val modelLoader = new Sphinx3Loader("resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz",
                                      "mdef", "", logMath, unitManager, 0.0f, 1e-7f, 0.0001f, true)

  val model = new TiedStateAcousticModel(modelLoader, unitManager, true)

//  val dictionary = new FastDictionary("resource:/edu/cmu/sphinx/model/acoustic/HUB4_8gau_13dCep_16k_40mel_133Hz_6855Hz/cmudict.06d",
//                                      "resource:/edu/cmu/sphinx/model/acoustic/HUB4_8gau_13dCep_16k_40mel_133Hz_6855Hz/fillerdict",
//                                      Seq[URL](), false, "<sil>", false, false, unitManager)
  
  val dictionary = new FastDictionary(
                "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/dict/cmudict.0.6d",
                "resource:/WSJ_8gau_13dCep_16k_40mel_130Hz_6800Hz/noisedict",
                Seq[URL](),
                false,
                "<sil>",
                false,
                false,
                unitManager)
  
//  val languageModel = new LargeNGramModel(
//    "DMP",
//    ConfigurationManagerUtils.resourceToURL("src/main/resources/language_model.arpaformat.DMP"),
//    null, // ngramLogFile
//    100000, //cacheSize
//    false,
//    -1,
//    logMath,
//    dictionary,
//    false,
//    1.0f,
//    1.0f,
//    1.0f,
//    false
//  )
  
  val languageModel = new SimpleNGramModel(
    "src/main/resources/hellongram.trigram.lm",
    dictionary, // dictionary
    0.7f, // unigramWeight,
    logMath, // logMath,
    3 // desiredMaxDepth,
  )

  val linguist = new LexTreeLinguist(
    model, // AcousticModel acousticModel,
    logMath, // LogMath logMath,
    unitManager, // UnitManager unitManager,
    languageModel,
    dictionary, // dictionary,
    true, //boolean fullWordHistories,
    true, // wantUnigramSmear,
    wordInsertionProbability, // wordInsertionProbability,
    0.1f, // double silenceInsertionProbability,
    1E-10, // fillerInsertionProbability,
    1.0, // unitInsertionProbability,
    languageWeight, // languageWeight,
    false, // addFillerWords,
    false, // generateUnitStates,
    1.0f, // unigramSmearWeight,
    0 // arcCacheSize
  )

  val searchManager = new SimpleBreadthFirstSearchManager(
          logMath, linguist, pruner,
          scorer, activeListFactory,
          false, 0.0, 0, false)

  val decoder = new Decoder(searchManager,
          false, false,
          Seq[ResultListener](),
          100000)
  
  val monitors = new java.util.ArrayList[Monitor]()
  
  val recognizer = new Recognizer(decoder, monitors)

  // Yuck
  monitors.add(new BestPathAccuracyTracker(recognizer, false, false, false, false, false, false))
  monitors.add(new MemoryTracker(recognizer, false, false))
  monitors.add(new SpeedTracker(recognizer, frontend, true, false, false, false))
                     
}