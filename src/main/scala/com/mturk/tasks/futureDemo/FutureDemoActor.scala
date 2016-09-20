package com.mturk.tasks.futureDemo

import java.io.StringReader

import akka.actor.{Actor, ActorLogging}
import edu.stanford.nlp.ling.HasWord
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.process.DocumentPreprocessor
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.{TregexParseException, TregexPattern}
import org.json4s.JsonAST.JObject

import scala.collection.mutable

/**
 * Created by Aimingnie on 7/20/15
 */
class FutureDemoActor extends Actor with ActorLogging {

  import FutureDemoProtocol._
  import FutureRules._
  import com.mturk.tasks.Util._

  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", "-MAX_ITEMS","50000")

  val tnterms = List("today","tonight","tonite","yesterday","yest","tomorrow","tom","tmrw","morning","morn","evening","night","nite","midnight","midnite","now")
  val tcterms = List("awhile","while","second","sec","minute","min","half-hour","half-hr","hour","hr","day","week","wk","weekend","wknd","month","mo","year","yr","decade","dec","century","cent","C","millennium","moment","instant","flash","millisecond","ms","microsecond","season","spring","summer","fall","autumn","winter","semester","sem","trimester","term","quarter","qtr")

  val dRF = patternFuture2_6.map(e => getRule(e))
  val dRP = patternPast.map(e => getRule(e))

  override def receive = {
    case JObjectSentences(jObject) =>
      if (jObject.isDefined("sentences")) {
        val rawSentences = jObject.getValue("sentences")

        val userRulesF:List[Option[(String,Option[TregexPattern])]] = jObject.values.get("userRulesFuture") match {
          case Some(r:List[String]) => r.map(e => getRule(e))
          case Some(_) => List[Option[(String,Option[TregexPattern])]]()
          case None => List[Option[(String,Option[TregexPattern])]]()
        }

        val userRulesP:List[Option[(String,Option[TregexPattern])]] = jObject.values.get("userRulesPast") match {
          case Some(r:List[String]) => r.map(e => getRule(e))
          case Some(_) => List[Option[(String, Option[TregexPattern])]]()
          case None => List[Option[(String,Option[TregexPattern])]]()
        }

        //first break it down to tokenizer
        val sentences = tokenize(rawSentences)

        //then parse them
        val parsedSentences = sentences.map(sen => (sen, lp.parse(sen)))

        //then match them
        val matchedResult = parsedSentences.map(
          tuple => MatchedResult(tuple._1, tuple._2.toString, search(
            tuple._2, if(userRulesF.isEmpty) dRF else userRulesF, if(userRulesP.isEmpty) dRP else userRulesP)))



        sender ! TransOk(Some(matchedResult), succeedOrNot = true, None)

      } else {
        sender ! TransOk(None, succeedOrNot = false, Some("must define a JSON field 'sentences'"))
      }
  }

  private[this] def getRule(r: String): Option[(String, Option[TregexPattern])] = {
    try {
      val compiledRule = TregexPattern.compile(preprocessTNTC(r))
      Some(r, Some(TregexPattern.compile(preprocessTNTC(r))))
    }
    catch {
      case e: TregexParseException =>
//        println("Bad rule by user : " + preprocessTNTC(r))
        Some(r, None)
    }
  }

  private[this] def preprocessTNTC(pattern: String): String = {
    if(pattern.contains("TN|TC") || pattern.contains("TC|TN")) {
      return pattern.replace("TN", tnterms.mkString("|")).replace("TC", tcterms.mkString("|"))
    }
    if (pattern.contains("TN")) {
      return pattern.replace("TN", tnterms.mkString("|"))
    }
    if (pattern.contains("TC")) {
      return pattern.replace("TC", tcterms.mkString("|"))
    }
    pattern
  }

  private[this] def search(tree: Tree, futureRules: List[Option[(String, Option[TregexPattern])]], past: List[Option[(String, Option[TregexPattern])]]): ResultArray = {
    val futureStats =  Array.fill[Int](futureRules.size)(0)

    val malformedTrees = mutable.ArrayBuffer[String]()
    val unparsedRules = mutable.ArrayBuffer[String]()

    //TODO Test if there's None on rule
    futureRules.flatten.indices.foreach { i =>
      try {
        if (futureRules(i).get._2.nonEmpty) {
          val matcher = futureRules(i).get._2.get.matcher(tree)
          if (matcher.find()) {
            futureStats(i) = futureStats(i) + 1
          }
        }
        else {
          unparsedRules += futureRules(i).get._1
        }

      } catch {
        case e: NullPointerException =>
          //this happens when a tree is malformed
          //we will not add any number to stats, just return it as is
//          println("NULL Pointer with " + tree.toString)
          malformedTrees.append(tree.toString)
      }
    }

    val pastStats =  Array.fill[Int](past.size)(0)

    past.flatten.indices.foreach { i =>
      try {
        if (past(i).get._2.nonEmpty) {
          val matcher = past(i).get._2.get.matcher(tree)
          if (matcher.find()) {
            pastStats(i) = pastStats(i) + 1
          }
        }
        else {
          unparsedRules += past(i).get._1
        }
      } catch {
        case e: NullPointerException =>
          //this happens when a tree is malformed
          //we will not add any number to stats, just return it as is
          println("NULL Pointer with " + tree.toString)
      }
    }

    ResultArray(futureStats.sum, mutable.HashMap(futureRules.flatten.map(e => e._1).zip(futureStats): _*),
      pastStats.sum, mutable.HashMap(past.flatten.map(e => e._1).zip(pastStats): _*), malformedTrees, unparsedRules)
  }


  private[this] def tokenize(sentences: String): mutable.Queue[String] = {

    val tempRows = mutable.Queue.empty[String]

    val processorIt = new DocumentPreprocessor(new StringReader(sentences))

    val pit = processorIt.iterator()

    while (pit.hasNext) {
      val sentence = formSentence(pit.next())
      tempRows.enqueue(sentence)
    }

    tempRows
  }

  private[this] def formSentence(list: java.util.List[HasWord]): String = {
    var sentence = ""
    val it = list.iterator()
    while (it.hasNext)
      sentence += it.next().word() + " "
    sentence
  }
}

object FutureDemoProtocol {
  import scala.collection.mutable
  case class JObjectSentences(jObject: JObject)
  case class MatchedResult(sen: String, parsed: String, result: ResultArray)
  case class ResultArray(future: Int, futureResult: mutable.HashMap[String, Int], past: Int, pastResult: mutable.HashMap[String, Int],
                        malformedTrees:mutable.ArrayBuffer[String], unparsedRules:mutable.ArrayBuffer[String])
  case class TransOk(sentencesResult: Option[mutable.Queue[MatchedResult]], succeedOrNot: Boolean, errorMessage: Option[String])
}
