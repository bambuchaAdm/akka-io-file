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
import sun.nio.ByteBuffered

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

  val defaultFileLength: Int = 64

  def withExampleFileContent[A](contentLength: Int = defaultFileLength)(f: (Path, ByteString) => A): A = {
    val path = Files.createTempFile(temporaryPath, suiteName, null)
    val channel = FileChannel.open(path, CREATE, WRITE, TRUNCATE_EXISTING)
    val content = randomString(contentLength)
    val bytes = content.getBytes(charset)
    channel.write(ByteBuffer.wrap(bytes))
    channel.force(true)
    f(path, ByteString(bytes))
  }

  def randomString(length: Int): String = {
    new String((1 to length).map { _ => Random.nextPrintableChar() }.toArray)
  }
}

trait FileActorFixtures extends FileFixtures with DefaultSender {
  self: Suite with TestKitBase =>

  def withDescriptorAndContent[A](f: (ActorRef, ByteString) => A): A = {
    withExampleFileContent(){ (path, content) =>
      IO(File) ! Open(path)
      val handler = expectMsgClass(5.seconds, classOf[ActorRef])
      f(handler, content)
    }
  }

  def withDescriptor[A, B](path: Path)(f: ActorRef => A): A = {
    IO(File) ! Open(path, CREATE, READ, WRITE)
    val handler = expectMsgClass(classOf[ActorRef])
    f(handler)
  }

  def withDescriptorAndContent[A, B](path: Path)(f: ActorRef => A)(g: ByteString => B): B = {
    withDescriptor(path)(f)
    withFileContent(path)(g)
  }

  def withFileContent[A](path: Path)(f: ByteString => A): A = {
    val channel = FileChannel.open(path, READ)
    val buffer = ByteBuffer.allocateDirect(channel.size().toInt)
    channel.read(buffer)
    buffer.rewind()
    f(ByteString(buffer))
  }
}

