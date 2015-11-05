package akka.io

import java.nio.file.{StandardOpenOption, Files, OpenOption}
import java.util.Collections
import java.util.concurrent.{AbstractExecutorService, TimeUnit}

import akka.actor.{OneForOneStrategy, SupervisorStrategy, Actor, Props}
import akka.io.File._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

/**
  * Created by bambucha on 05.11.15.
  */
class FileManager(extension: FileExt) extends Actor  {

  val executorService = ExecutionContextExecutorServiceBridge(extension.fileExecutionContext)

  override def receive: Receive = {
    case command: Open =>
      if(Files.exists(command.path) || command.openOption.contains(StandardOpenOption.CREATE)) {
        val openOptions: Set[_ <: OpenOption] = command.openOption.toSet
        sender() ! context.actorOf(Props(classOf[FileDescriptor], command.path, openOptions, executorService, extension.bufferPool))
      } else {
        sender() ! FileNotFound
      }
  }
}


object ExecutionContextExecutorServiceBridge {
  def apply(ec: ExecutionContext): ExecutionContextExecutorService = ec match {
    case null => throw new NullPointerException("execution context is null")
    case eces: ExecutionContextExecutorService => eces
    case other => new AbstractExecutorService with ExecutionContextExecutorService {
      override def prepare(): ExecutionContext = other
      override def isShutdown = false
      override def isTerminated = false
      override def shutdown() = ()
      override def shutdownNow() = Collections.emptyList[Runnable]
      override def execute(runnable: Runnable): Unit = other execute runnable
      override def reportFailure(t: Throwable): Unit = other reportFailure t
      override def awaitTermination(length: Long, unit: TimeUnit): Boolean = false
    }
  }
}
