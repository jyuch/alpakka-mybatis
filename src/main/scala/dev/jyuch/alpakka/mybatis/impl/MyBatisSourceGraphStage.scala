package dev.jyuch.alpakka.mybatis.impl

import akka.annotation.InternalApi
import akka.event.Logging
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.SqlSession

@InternalApi private[mybatis] final class MyBatisSourceGraphStage[T](
  sessionFactory: () => SqlSession,
  cursorFactory: SqlSession => Cursor[T]
) extends GraphStage[SourceShape[T]] {
  val out: Outlet[T] = Outlet(Logging.simpleName(this) + ".out")
  override val shape: SourceShape[T] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler {

      var session: SqlSession = _
      var cursor: Cursor[T] = _
      var iterator: java.util.Iterator[T] = _

      setHandler(out, this)

      override def preStart(): Unit = {
        super.preStart()
        session = sessionFactory()
        cursor = cursorFactory(session)
        iterator = cursor.iterator()
      }

      override def postStop(): Unit = {
        if (session == null) {
          session.close()
        }
        if (cursor != null) {
          cursor.close()
        }
        super.postStop()
      }

      override def onPull(): Unit = {
        if (iterator.hasNext) {
          push(out, iterator.next())
        } else {
          completeStage()
        }
      }

      override def onDownstreamFinish(cause: Throwable): Unit = {
        completeStage()
        super.onDownstreamFinish(cause)
      }
    }
}
