package akka.io

import java.nio.file.{OpenOption, Path}

import akka.actor._
import akka.util.ByteString

/**
  * Created by bambucha on 05.11.15.
  */
object File extends ExtensionId[FileExt] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): FileExt = new FileExt(system)

  override def lookup(): ExtensionId[_ <: Extension] = File

  class CommandFailed(val exception: Throwable)

  case class Open(path: Path, openOption: OpenOption*)

  case class FileNotFound(override val exception: Throwable) extends CommandFailed(exception)

  case class Read(position: Long, length: Int)

  case class ReadResult(result: ByteString, position: Long, length: Int)

  case class OutOfFileBoundaryRead(position: Long, length: Int)

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
