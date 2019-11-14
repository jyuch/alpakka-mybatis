package dev.jyuch.alpakka.mybatis.impl

import akka.annotation.InternalApi
import akka.event.Logging
import akka.stream.SubscriptionWithCancelException.StageWasCompleted
import akka.stream._
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, OutHandler}
import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.SqlSession

import scala.concurrent.{Future, Promise}
import scala.util.Success
import scala.util.control.NonFatal

@InternalApi private[mybatis] final class MyBatisSourceGraphStage[Out](
  sessionFactory: () => SqlSession,
  cursorFactory: SqlSession => Cursor[Out]
) extends GraphStageWithMaterializedValue[SourceShape[Out], Future[IOResult]] {
  val out: Outlet[Out] = Outlet(Logging.simpleName(this) + ".out")
  override val shape: SourceShape[Out] = SourceShape(out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[IOResult]) = {
    val mat = Promise[IOResult]
    val logic: GraphStageLogic with OutHandler = new GraphStageLogic(shape) with OutHandler {

      var session: SqlSession = _
      var cursor: Cursor[Out] = _
      var iterator: java.util.Iterator[Out] = _
      var read: Int = 0

      setHandler(out, this)

      override def preStart(): Unit = {
        super.preStart()
        try {
          session = sessionFactory()
          cursor = cursorFactory(session)
          iterator = cursor.iterator()
        } catch {
          case NonFatal(t) =>
            closeSession(Some(new IOOperationIncompleteException(read, t)))
            failStage(t)
        }
      }

      override def postStop(): Unit = {
        if (!mat.isCompleted) {
          val failure = new AbruptStageTerminationException(this)
          closeSession(Some(failure))
          mat.tryFailure(failure)
        }
        super.postStop()
      }

      override def onPull(): Unit = {
        try {
          if (iterator.hasNext) {
            emit(out, iterator.next())
            read += 1
          } else {
            completeStage()
            closeSession(None)
          }
        }
        catch {
          case NonFatal(t) =>
            failStage(t)
        }
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
        cause match {
          case StageWasCompleted =>
            closeSession(None)
          case _ =>
            closeSession(Option(cause))
            super.onDownstreamFinish(cause)
        }
      }

      private def closeSession(failed: Option[Throwable]): Unit = {
        try {
          if (session ne null) session.close()
          failed match {
            case Some(t) => mat.tryFailure(t)
            case None => mat.tryComplete(Success(IOResult(read)))
          }
        } catch {
          case NonFatal(t) =>
            mat.tryFailure(failed.getOrElse(t))
        }
      }
    }
    (logic, mat.future)
  }
}
