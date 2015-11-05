package akka.io

import java.io.File
import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._

import akka.actor.ActorRef
import akka.io.File._

/**
  * Created by bambucha on 05.11.15.
  */
class FileDescriptorTest extends BaseTest with FileActorFixtures {

  behavior of classOf[FileDescriptor].getSimpleName

  it should "read content file" in {
    withDescriptorAndContent{ (handler, content) =>
      val from = 16
      val length = 32
      handler ! Read(from,length)
      expectMsg(ReadResult(content.slice(from,from+length), from, length))
    }
  }

  it should "send information about read out of file boundary" in {
    withDescriptorAndContent { (handler, content) =>
      handler ! Read(defaultFileLength + 16, 16)
      expectMsg(OutOfFileBoundaryRead(defaultFileLength + 16, 16))
    }
  }

  it should "send information about fail on trying read non existing file" in {

  }
}
