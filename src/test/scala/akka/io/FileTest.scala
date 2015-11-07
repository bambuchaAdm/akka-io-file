package akka.io

import java.io.{File => JFile}
import java.nio.file.{NoSuchFileException, Path}
import java.nio.file.StandardOpenOption._

import akka.actor.ActorRef
import akka.io.File.{CommandFailed, Open}

/**
  * Created by bambucha on 05.11.15.
  */
class FileTest extends BaseTest with FileFixtures {

  behavior of "File Extension"

  it should "be defined" in {
    IO(File) shouldBe an[ActorRef]
  }

  it should "send back open file descriptor" in {
    withExampleFile { file =>
      IO(File) ! Open(file)
      expectMsgClass(classOf[ActorRef])
    }
  }

  it should "send back error about file not found" in {
    val nonExistingFileName = "non-existing-file"
    val path: Path = new JFile(nonExistingFileName).toPath
    IO(File) ! Open(path, READ)
    val fail = expectMsgType[CommandFailed]
    fail.reason shouldBe a[NoSuchFileException]
  }
}
