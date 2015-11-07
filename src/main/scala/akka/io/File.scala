package akka.io

import java.nio.file.{OpenOption, Path}
import javax.swing.text.Position

import akka.actor._
import akka.util.ByteString

/**
  * Created by bambucha on 05.11.15.
  */
object File extends ExtensionId[FileExt] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): FileExt = new FileExt(system)

  override def lookup(): ExtensionId[_ <: Extension] = File

  trait Event

  final case class CommandFailed(command: Command, reason: Throwable) extends Event

  trait Command {
    def failureMessage(reason: Throwable) = CommandFailed(this, reason)
  }

  case class Open(path: Path, openOption: OpenOption*) extends Command

  case class Read(position: Long, length: Int) extends Command

  case class ReadResult(data: ByteString, position: Long, length: Int) extends Event

  case class OutOfFileBoundaryRead(read: Read) extends Event

  case class Write(position: Long, data: ByteString) extends Command

  case class Wrote(position: Long, data: ByteString) extends Event

}

class FileExt(system: ExtendedActorSystem) extends IO.Extension {

  val settings: Settings = new Settings

  class Settings {
    val config = system.settings.config.getConfig("akka.io.file")

    val ChannelDispatcher = config.getString("channel-dispatcher")

    val DefaultBufferSize = config.getInt("default-buffer-size")

    val BufferPoolSize = config.getInt("buffer-pool-size")
  }

  val bufferPool: BufferPool = new DirectByteBufferPool(settings.DefaultBufferSize, settings.BufferPoolSize)

  val fileExecutionContext = system.dispatchers.defaultGlobalDispatcher

  override val manager: ActorRef = system.systemActorOf(Props(classOf[FileManager], this), "IO-File")
}
