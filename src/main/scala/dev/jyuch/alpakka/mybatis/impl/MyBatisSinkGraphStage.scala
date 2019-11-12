package dev.jyuch.alpakka.mybatis.impl

import akka.NotUsed
import akka.annotation.InternalApi
import akka.event.Logging
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{AbruptStageTerminationException, Attributes, IOOperationIncompleteException, IOResult, Inlet, SinkShape}
import org.apache.ibatis.session.SqlSession

import scala.concurrent.{Future, Promise}
import scala.util.Success
import scala.util.control.NonFatal

@InternalApi private[mybatis] final class MyBatisSinkGraphStage[T](
  sessionFactory: () => SqlSession,
  operationFactory: SqlSession => (T => Any),
  commitEachItem: Boolean
) extends GraphStageWithMaterializedValue[SinkShape[T], Future[IOResult]] {
  val in: Inlet[T] = Inlet(Logging.simpleName(this) + ".in")
  override val shape: SinkShape[T] = SinkShape(in)

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[IOResult]) = {
    val mat = Promise[IOResult]
    val logic = new GraphStageLogic(shape) with InHandler {

      var session: SqlSession = _
      var operation: (T => Any) = _
      var inserted: Int = 0

      setHandler(in, this)

      override def preStart(): Unit = {
        super.preStart()
        try {
          session = sessionFactory()
          operation = operationFactory(session)
          pull(in)
        } catch {
          case NonFatal(t) =>
            closeSession(Some(new IOOperationIncompleteException(inserted, t)))
            failStage(t)
        }
      }

      override def onPush(): Unit = {
        val next = grab(in)
        try {
          operation(next)
          inserted += 1
          if (commitEachItem) {
            session.commit()
          }
          pull(in)
        } catch {
          case NonFatal(t) =>
            closeSession(Some(new IOOperationIncompleteException(inserted, t)))
            failStage(t)
        }
      }

      override def onUpstreamFailure(t: Throwable): Unit = {
        closeSession(Some(new IOOperationIncompleteException(inserted, t)))
        failStage(t)
      }

      override def onUpstreamFinish(): Unit = {
        closeSession(None)
        completeStage()
      }

      override def postStop(): Unit = {
        if (!mat.isCompleted) {
          val failure = new AbruptStageTerminationException(this)
          closeSession(Some(failure))
          mat.tryFailure(failure)
        }
      }

      private def closeSession(failed: Option[Throwable]): Unit = {
        try {
          if (session ne null) session.close()
          failed match {
            case Some(t) => mat.tryFailure(t)
            case None => mat.tryComplete(Success(IOResult(inserted)))
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
