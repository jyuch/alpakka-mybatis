package dev.jyuch.alpakka.mybatis.impl

import akka.annotation.InternalApi
import akka.event.Logging
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import org.apache.ibatis.session.SqlSession

import scala.util.control.NonFatal

@InternalApi private[mybatis] final class MyBatisFlowGraphStage[In, Out](
  sessionFactory: () => SqlSession,
  action: (SqlSession, In) => Out
) extends GraphStage[FlowShape[In, Out]] {
  val in: Inlet[In] = Inlet(Logging.simpleName(this) + ".in")
  val out: Outlet[Out] = Outlet(Logging.simpleName(this) + ".out")
  override val shape: FlowShape[In, Out] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with InHandler with OutHandler {

      var session: SqlSession = _

      setHandlers(in, out, this)

      override def preStart(): Unit = {
        try {
          session = sessionFactory()
        } catch {
          case NonFatal(t) =>
            closeSession()
            failStage(t)
        }
      }

      override def onPush(): Unit = {
        val next = grab(in)
        try {
          emit(out, action(session, next))
        } catch {
          case NonFatal(t) =>
            closeSession()
            failStage(t)
        }
      }

      override def onPull(): Unit = {
        pull(in)
      }

      override def onUpstreamFailure(t: Throwable): Unit = {
        closeSession()
        failStage(t)
      }

      override def onUpstreamFinish(): Unit = {
        closeSession()
        completeStage()
      }

      private def closeSession(): Unit = {
        if (session ne null) session.close()
      }
    }
}
