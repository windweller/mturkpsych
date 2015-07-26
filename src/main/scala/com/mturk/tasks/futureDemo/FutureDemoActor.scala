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

/**
 * Created by Aimingnie on 7/20/15
 */
class FutureDemoActor extends Actor with ActorLogging {

  import FutureDemoProtocol._
  import FutureRules._
  import com.mturk.tasks.Util._

  val lp = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz", "-MAX_ITEMS","50000")
  val patterns = (patternFuture2_2_2 ++ patternsPast).map(e => TregexPattern.compile(e))

  override def receive = {
    case JObjectSentences(jObject) =>
      if (jObject.isDefined("sentences")) {
        val rawSentences = jObject.getValue("sentences")
        //first break it down to tokenizer
        val sentences = tokenize(rawSentences)

        //then parse them
        val parsedSentences = sentences.map(sen => (sen, lp.parse(sen)))

        //then match them
        val matchedResult = parsedSentences.map(tuple => MatchedResult(tuple._1, search(tuple._2)))

        sender ! TransOk(Some(matchedResult), succeedOrNot = true, None)

      } else {
        sender ! TransOk(None, succeedOrNot = false, Some("must define a JSON field 'sentences'"))
      }
  }

  private[this] def search(tree: Tree): Array[Int] = {
    val stats =  Array.fill[Int](patterns.size)(0)

    patterns.indices.foreach { i =>

      try {
        val matcher = patterns(i).matcher(tree)
        if (matcher.find()) {
          stats(i) = stats(i) + 1
        }
      } catch {
        case e: NullPointerException =>
          //this happens when a tree is malformed
          //we will not add any number to stats, just return it as is
          println("NULL Pointer with " + tree.toString)
      }
    }
    stats
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
  case class MatchedResult(sen: String, result: Array[Int])
  case class TransOk(sentencesResult: Option[mutable.Queue[MatchedResult]], succeedOrNot: Boolean, errorMessage: Option[String])
}
