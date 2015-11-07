package akka.io

import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.{OpenOption, Path}

import akka.actor.{Actor, ActorRef}
import akka.io.File._
import akka.util.{ByteString, ByteStringBuilder}

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContextExecutorService
import scala.util.control.NonFatal

class FileDescriptor(path: Path, options: Set[_ <: OpenOption], executionService: ExecutionContextExecutorService, pool: BufferPool) extends Actor{

  var channel : AsynchronousFileChannel = _

  @throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    self ! Open
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    if(channel != null){
      channel.close()
    }
  }

  override def receive: Receive = {
    case msg: Open =>
      try {
        channel = AsynchronousFileChannel.open(path, options.asJava, executionService)
        sender() ! self
      } catch {
        case ex if NonFatal(ex) =>
          sender() ! msg.failureMessage(ex)
          context.stop(self)
      }
    case read @ Read(position, length) =>
      val buffer = pool.acquire()
      buffer.limit(Math.min(buffer.capacity(), length))
      channel.read(buffer, position, sender(), new ReadCompletionHandler(new ByteStringBuilder, buffer, read))
    case write @ Write(position, data) =>
      val buffers = data.asByteBuffers
      channel.write(buffers.head, position, sender(), new WriteCompletionHandler(0, buffers, write))

  }

  abstract class DefaultCompletionHandler[A](command: Command) extends CompletionHandler[A, ActorRef] {
    override def failed(throwable: Throwable, a: ActorRef): Unit = {
      a ! command.failureMessage(throwable)
    }
  }

  class ReadCompletionHandler(builder: ByteStringBuilder, buffer: ByteBuffer, read: Read) extends DefaultCompletionHandler[Integer](read) {
    override def completed(result: Integer, actor: ActorRef): Unit = {
      if(result != -1) {
        buffer.flip()
        builder ++= ByteString(buffer)
        if(builder.length == read.length) {
          actor ! ReadResult(builder.result, read.position, read.length)
        } else {
          buffer.clear()
          channel.read(buffer, read.position + builder.length, actor, this)
        }
      } else {
        actor ! OutOfFileBoundaryRead(read)
        pool.release(buffer)
      }
    }
  }

  class WriteCompletionHandler(var written: Int, var buffers: Traversable[ByteBuffer], write: Write) extends DefaultCompletionHandler[Integer](write){
    override def completed(result: Integer, actor: ActorRef): Unit = {
      if(buffers.isEmpty){
        actor ! Wrote(write.position, write.data)
      } else {
        written += result
        channel.write(buffers.head, write.position + written, actor, this)
        buffers = buffers.tail
      }
    }
  }
}