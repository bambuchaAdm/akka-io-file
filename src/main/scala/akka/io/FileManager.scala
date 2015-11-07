package akka.io

import java.io.IOException
import java.nio.file.{NoSuchFileException, OpenOption}
import java.util.Collections
import java.util.concurrent.{AbstractExecutorService, TimeUnit}

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.io.File._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

class FileManager(extension: FileExt) extends Actor  {

  val executorService = ExecutionContextExecutorServiceBridge(extension.fileExecutionContext)

  override def receive: Receive = {
    case command: Open =>
      val openOptions: Set[_ <: OpenOption] = command.openOption.toSet
      val actor = context.actorOf(descriptorProps(command, openOptions))
      actor.forward(command)
  }

  def descriptorProps(command: Open, openOptions: Set[_ <: OpenOption]): Props = {
    Props(classOf[FileDescriptor], command.path, openOptions, executorService, extension.bufferPool)
  }

  val decider: Decider = {
    case ex: IOException          => Resume
  }

  override def supervisorStrategy: SupervisorStrategy = new OneForOneStrategy()(decider){
    override def processFailure(context: ActorContext, restart: Boolean, child: ActorRef, cause: Throwable, stats: ChildRestartStats, children: Iterable[ChildRestartStats]): Unit = {
      super.processFailure(context, restart, child, cause, stats, children)
      println(cause)
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
