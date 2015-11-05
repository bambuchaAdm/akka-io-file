package akka.io

import java.nio.file.{Files, Path}

import akka.actor.ActorSystem
import akka.testkit.TestKitBase
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, Suite}

/**
  * Created by bambucha on 05.11.15.
  */
abstract class BaseTest extends FlatSpec with TestActorSystem with TestKitBase  with Matchers with DefaultSender

trait TestActorSystem {
  self: Suite with TestKitBase=>

  override implicit val system = ActorSystem(suiteName)
}

trait DefaultSender {
  self: TestKitBase =>

  implicit val sender = testActor
}

