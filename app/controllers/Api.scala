package controllers

import javax.inject.Inject

import com.madgag.github.RepoId
import lib._
import lib.model.MessageSummary
import play.api.cache.Cached
import play.api.libs.json.Json._
import play.api.mvc._

import scala.Function.unlift
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class Api @Inject() (cached: Cached) extends Controller {

  val MessageFoundHeader = "X-submitGit-MessageFound"
  
  val CacheForLongerIfMessageFound = unlift[ResponseHeader, Duration](_.headers.get(MessageFoundHeader).map(_ => 100 days))
  
  def messageLookup(repoId: RepoId, query: String) = {
    cached.apply((_ => s"$repoId $query"): (RequestHeader => String), CacheForLongerIfMessageFound).default(3 seconds) {
      Action.async {
        for {
          messagesOpt <- Project.byRepoId(repoId).mailingList.lookupMessage(query)
        } yield {
          Ok(toJson(messagesOpt: Seq[MessageSummary])).withHeaders(messagesOpt.map(_ => MessageFoundHeader -> "true") : _*)
        }
      }
    }
  }

}



