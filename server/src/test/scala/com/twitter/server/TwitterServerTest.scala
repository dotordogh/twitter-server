package com.twitter.server

import com.twitter.conversions.time._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable

class TestTwitterServer extends TwitterServer {
  override val defaultAdminPort = 0
  /* ensure enough time to close resources */
  override val defaultCloseGracePeriod: Duration = 30.seconds

  val bootstrapSeq: mutable.MutableList[Symbol] = mutable.MutableList.empty[Symbol]

  def main(): Unit = {
    bootstrapSeq += 'Main
  }

  init {
    bootstrapSeq += 'Init
  }

  premain {
    bootstrapSeq += 'PreMain
  }

  onExit {
    bootstrapSeq += 'Exit
  }

  postmain {
    bootstrapSeq += 'PostMain
  }
}

class MockExceptionHandler extends Service[Request, Response] {
  val pattern = "/exception_please.json"
  def apply(req: Request): Future[Response] = {
    throw new Exception("test exception")
  }
}
@RunWith(classOf[JUnitRunner])
class TwitterServerTest extends FunSuite {
  test("TwitterServer does not prematurely execute lifecycle hooks") {
    val twitterServer = new TestTwitterServer
    assert(twitterServer.bootstrapSeq.isEmpty)
  }

  test("TwitterServer.main(args) executes without error") {
    val twitterServer = new TestTwitterServer
    twitterServer.main(args = Array.empty[String])
    assert(
      twitterServer.bootstrapSeq ==
        Seq('Init, 'PreMain, 'Main, 'PostMain, 'Exit)
    )
  }

  test("TwitterServer.main(args) executes without error when closed explicitly") {
    val twitterServer = new TestTwitterServer {
      override def main(): Unit = {
        super.main()
        Await.result(close())
      }
    }

    twitterServer.main(args = Array.empty[String])
    assert(twitterServer.bootstrapSeq == Seq('Init, 'PreMain, 'Main, 'Exit, 'PostMain))
  }
}