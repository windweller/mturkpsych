package com.mturk.tasks.futureDemo

import java.io.StringReader

import akka.actor.{Actor, ActorLogging}
import edu.stanford.nlp.ling.HasWord
import edu.stanford.nlp.parser.lexparser.LexicalizedParser
import edu.stanford.nlp.process.DocumentPreprocessor
import edu.stanford.nlp.trees.Tree
import edu.stanford.nlp.trees.tregex.TregexPattern
import org.json4s.JsonAST.JObject

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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

        //TODO gestion d'erreurs des règles passées
        val userRulesF = jObject.values.get("userRulesFuture") match {
          case Some(r:List[String]) => r.map(e => getRule(e))
          case Some(_) => List[(String, TregexPattern)]()
          case None => List[(String, TregexPattern)]()
        }

        val userRulesP = jObject.values.get("userRulesPast") match {
          case Some(r:List[String]) => r.map(e => getRule(e))
          case Some(_) => List[(String, TregexPattern)]()
          case None => List[(String, TregexPattern)]()
        }

        //first break it down to tokenizer
        val sentences = tokenize(rawSentences)

        //then parse them
        val parsedSentences = sentences.map(sen => (sen, lp.parse(sen)))

        //then match them
        val matchedResult = parsedSentences.map(tuple => MatchedResult(tuple._1, tuple._2.toString, search(tuple._2, userRulesF, userRulesP)))

        sender ! TransOk(Some(matchedResult), succeedOrNot = true, None)

      } else {
        sender ! TransOk(None, succeedOrNot = false, Some("must define a JSON field 'sentences'"))
      }
  }

  private[this] def getRule(r: String) = Tuple2(r, TregexPattern.compile(preprocessTNTC(r)))

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

  private[this] def preprocessTregex(patterns: List[String]): List[String] = {
    val result = ListBuffer.empty[String]
    patterns.foreach { p =>
      if (p.contains("TN|TC")) {
        //just replace all TN and TC
        //we are not replacing TN right now
        result ++= tnterms.map(tn => p.replace("TN|TC", tn))
        result ++= tcterms.map(tc => p.replace("TN|TC", tc))
      }
      else if (p.contains("TC|TN")) {
        result ++= tnterms.map(tn => p.replace("TC|TN", tn))
        result ++= tcterms.map(tc => p.replace("TC|TN", tc))
      }
      else if (p.contains("TN")) {
        result ++= tnterms.map(tn => p.replace("TN", tn))
      }
      else if (p.contains("TC")) {
        result ++= tcterms.map(tc => p.replace("TC", tc))
      }
      else result += p
    }
    result.toList
  }

  private[this] def search(tree: Tree, future: List[(String, TregexPattern)], past: List[(String, TregexPattern)]): ResultArray = {
    val futureStats =  Array.fill[Int](future.size)(0)

    future.indices.foreach { i =>
      try {
        val matcher = future(i)._2.matcher(tree)
        if (matcher.find()) {
          futureStats(i) = futureStats(i) + 1
        }
      } catch {
        case e: NullPointerException =>
          //this happens when a tree is malformed
          //we will not add any number to stats, just return it as is
          println("NULL Pointer with " + tree.toString)
      }
    }

    val pastStats =  Array.fill[Int](past.size)(0)

    past.indices.foreach { i =>
      try {
        val matcher = past(i)._2.matcher(tree)
        if (matcher.find()) {
          pastStats(i) = pastStats(i) + 1
        }
      } catch {
        case e: NullPointerException =>
          //this happens when a tree is malformed
          //we will not add any number to stats, just return it as is
          println("NULL Pointer with " + tree.toString)
      }
    }

    ResultArray(futureStats.sum, mutable.HashMap(future.map(e => e._1).zip(futureStats): _*),
      pastStats.sum, mutable.HashMap(past.map(e => e._1).zip(pastStats): _*))
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
  case class ResultArray(future: Int, futureResult: mutable.HashMap[String, Int], past: Int, pastResult: mutable.HashMap[String, Int])
  case class TransOk(sentencesResult: Option[mutable.Queue[MatchedResult]], succeedOrNot: Boolean, errorMessage: Option[String])
}
