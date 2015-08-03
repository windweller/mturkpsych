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

  val tnterms = List("today", "tomorrow", "tom", "tmrw")
  val tcterms = List("second", "sec", "minute", "min")

  val patternsFuture = preprocessTregex(patternFuture2_2_2).map(e => TregexPattern.compile(e))
  val patternsPast = preprocessTregex(patternPast).map(e => TregexPattern.compile(e))

  override def receive = {
    case JObjectSentences(jObject) =>
      if (jObject.isDefined("sentences")) {
        val rawSentences = jObject.getValue("sentences")
        //first break it down to tokenizer
        val sentences = tokenize(rawSentences)

        //then parse them
        val parsedSentences = sentences.map(sen => (sen, lp.parse(sen)))

        //then match them
        val matchedResult = parsedSentences.map(tuple => MatchedResult(tuple._1, tuple._2.toString, search(tuple._2)))

        sender ! TransOk(Some(matchedResult), succeedOrNot = true, None)

      } else {
        sender ! TransOk(None, succeedOrNot = false, Some("must define a JSON field 'sentences'"))
      }
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

  private[this] def search(tree: Tree): ResultArray = {
    val futureStats =  Array.fill[Int](patternsFuture.size)(0)

    patternsFuture.indices.foreach { i =>
      try {
        val matcher = patternsFuture(i).matcher(tree)
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

    val pastStats =  Array.fill[Int](patternsPast.size)(0)

    patternsPast.indices.foreach { i =>
      try {
        val matcher = patternsPast(i).matcher(tree)
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

    ResultArray(futureStats.sum, mutable.HashMap(preprocessTregex(patternFuture2_2_2).zip(futureStats): _*), pastStats.sum, mutable.HashMap(preprocessTregex(patternPast).zip(pastStats): _*))
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
