package akka.io

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{OpenOption, Path}

import akka.actor.{Actor, ActorRef}
import akka.io.File._
import akka.util.ByteString

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutorService

/**
  * Created by bambucha on 05.11.15.
  */
class FileDescriptor(path: Path, options: Set[_ <: OpenOption], executionService: ExecutionContextExecutorService, pool: BufferPool) extends Actor{

  var channel : AsynchronousFileChannel = _

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    channel = AsynchronousFileChannel.open(path, options.asJava, executionService)
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    channel.close()
  }

  override def receive: Receive = {
    case Read(position, length) =>
      val buffer = pool.acquire()
      buffer.limit(length)
      channel.read(buffer, position, sender(), ReadCompletionHandler(buffer, position, length))
  }

  abstract class DefaultCompletionHandler[A] extends CompletionHandler[A, ActorRef] {
    override def failed(throwable: Throwable, a: ActorRef): Unit = {
      a ! new CommandFailed(throwable)
    }
  }

  case class ReadCompletionHandler(buffer: ByteBuffer, position: Long, length: Int) extends DefaultCompletionHandler[Integer] {
    override def completed(v: Integer, a: ActorRef): Unit = {
      if(v != -1) {
        buffer.rewind()
        a ! ReadResult(ByteString(buffer), position, length)
      } else {
        a ! OutOfFileBoundaryRead(position, length)
      }
    }
  }
}

