package akka.io

import java.nio.file.{FileSystems, Path}

import akka.io.File._
import akka.util.ByteString

import scala.concurrent.duration._

class FileDescriptorTest extends BaseTest with FileActorFixtures with DefaultSender {

  behavior of classOf[FileDescriptor].getSimpleName

  it should "read content file" in {
    withDescriptorAndContent{ (handler, content) =>
      val from = 16
      val length = 32
      handler ! Read(from,length)
      expectMsg(ReadResult(content.slice(from,from+length), from, length))
    }
  }

  it should "read content file larger then buffer" in {
    withExampleFileContent(2048){ (path: Path, content: ByteString) =>
      withDescriptor(path){ handler =>
        handler ! Read(0, 2048)
        val result = expectMsgType[ReadResult]
        result.data shouldEqual content
      }
    }
  }

  it should "send information about read out of file boundary" in {
    withDescriptorAndContent { (handler, content) =>
      val read: Read = Read(defaultFileLength + 16, 16)
      handler ! read
      expectMsg(OutOfFileBoundaryRead(read))
    }
  }

  it should "write to file" in {
    val position = 16
    val buffer = ByteString("AAAAAAAA", "utf8")
    withExampleFileContent() { (path, originalContent) =>
      withDescriptorAndContent(path){ descriptor =>
        descriptor.tell(Write(position, buffer), testActor)
        expectMsg(10.seconds, Wrote(position, buffer))
      } { content =>
        content shouldEqual (originalContent.take(position) ++ buffer ++ originalContent.slice(position + buffer.size, originalContent.size))
      }
    }
  }

  it should "write to file byte string larger then default-buffer" in {
    val strings = (1 to 8).map( _ => ByteString(randomString(2048)))
    val exampleContent: ByteString = strings.fold(ByteString.empty){ _ ++ _}
    withExampleFile{ path =>
      withDescriptorAndContent(path){ handler =>
        handler ! Write(0, exampleContent)
        expectMsgType[Wrote]
      }{ content =>
        content shouldEqual exampleContent
      }
    }
  }

  it should "report error on full device" in {
    withDescriptor(FileSystems.getDefault.getPath("/", "dev", "full")){ handler =>
      handler ! Write(0, ByteString(randomString(16)))
      expectMsgType[CommandFailed]
    }
  }

}
