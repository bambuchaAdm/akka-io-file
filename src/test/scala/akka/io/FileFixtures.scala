package akka.io

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.{ Path, Files}
import java.nio.file.StandardOpenOption._

import akka.actor.ActorRef
import akka.io.File.Open
import akka.testkit.TestKitBase
import akka.util.{Timeout, ByteString}
import org.scalatest.{Suite, BeforeAndAfterAll}

import scala.util.Random
import scala.concurrent.duration._


trait FileFixtures extends BeforeAndAfterAll {
  self: Suite =>

  val temporaryPath = Files.createTempDirectory(suiteName)

  override protected def afterAll(): Unit = {
    super.afterAll()
    temporaryPath.toFile.delete()
  }

  def withExampleFile[A](f: Path => A): A = {
    f(Files.createTempFile(temporaryPath, suiteName, suiteId))
  }

  val charset: Charset = Charset.forName("utf8")

  val defaultFileLength: Int = 128

  def withExampleFileContent[A](f: (Path, ByteString) => A): A = {
    val path = Files.createTempFile(temporaryPath, suiteName, suiteId)
    val channel = FileChannel.open(path, CREATE, WRITE, TRUNCATE_EXISTING)
    val content = randomString(defaultFileLength)
    val bytes = content.getBytes(charset)
    channel.write(ByteBuffer.wrap(bytes))
    channel.force(false)
    f(path, ByteString(bytes))
  }

  def randomString(length: Int): String = {
    new String((1 to length).map { _ => Random.nextPrintableChar() }.toArray)
  }
}

trait FileActorFixtures extends FileFixtures with DefaultSender {
  self: Suite with TestKitBase =>

  def withDescriptorAndContent[A](f: (ActorRef, ByteString) => A): A = {
    withExampleFileContent{ (path, content) =>
      IO(File) ! Open(path)
      val handler = expectMsgClass(5.seconds, classOf[ActorRef])
      f(handler, content)
    }
  }
}

