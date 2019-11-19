package dev.jyuch.alpakka.mybatis.impl

import akka.annotation.InternalApi
import akka.event.Logging
import akka.stream.SubscriptionWithCancelException.StageWasCompleted
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}
import akka.stream._
import org.apache.ibatis.session.SqlSession

import scala.concurrent.{Future, Promise}
import scala.util.Success
import scala.util.control.NonFatal

@InternalApi private[mybatis] final class MyBatisFlowGraphStage[In, Out](
  sessionFactory: () => SqlSession,
  action: (SqlSession, In) => Out,
  commitAtStreamEnd: Boolean
) extends GraphStageWithMaterializedValue[FlowShape[In, Out], Future[IOResult]] {
  val in: Inlet[In] = Inlet(Logging.simpleName(this) + ".in")
  val out: Outlet[Out] = Outlet(Logging.simpleName(this) + ".out")
  override val shape: FlowShape[In, Out] = FlowShape(in, out)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[IOResult]) = {
    val mat = Promise[IOResult]
    val logic: GraphStageLogic with InHandler with OutHandler = new GraphStageLogic(shape) with InHandler with OutHandler {

      var session: SqlSession = _
      var income: Int = 0

      setHandlers(in, out, this)

      override def preStart(): Unit = {
        super.preStart()
        try {
          session = sessionFactory()
        } catch {
          case NonFatal(t) =>
            closeSession(Some(new IOOperationIncompleteException(income, t)))
            failStage(t)
        }
      }

      override def onPush(): Unit = {
        val next = grab(in)
        income += 1
        try {
          emit(out, action(session, next))
        } catch {
          case NonFatal(t) =>
            closeSession(Some(t))
            failStage(t)
        }
      }

      override def onPull(): Unit = {
        pull(in)
      }

      override def postStop(): Unit = {
        if (!mat.isCompleted) {
          val failure = new AbruptStageTerminationException(this)
          closeSession(Some(failure))
          mat.tryFailure(failure)
        }
        super.postStop()
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

      override def onUpstreamFailure(t: Throwable): Unit = {
        closeSession(Some(new IOOperationIncompleteException(income, t)))
        failStage(t)
      }

      override def onUpstreamFinish(): Unit = {
        closeSession(None)
        completeStage()
      }

      private def closeSession(failed: Option[Throwable]): Unit = {
        try {
          failed match {
            case Some(t) =>
              mat.tryFailure(t)
              if (commitAtStreamEnd) {
                session.rollback()
              }
            case None =>
              mat.tryComplete(Success(IOResult(income)))
              if (commitAtStreamEnd) {
                session.commit()
              }
          }
          if (session ne null) session.close()
        } catch {
          case NonFatal(t) =>
            mat.tryFailure(failed.getOrElse(t))
        }
      }
    }
    (logic, mat.future)
  }
}
