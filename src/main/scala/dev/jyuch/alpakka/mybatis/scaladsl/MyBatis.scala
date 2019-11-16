package dev.jyuch.alpakka.mybatis.scaladsl

import akka.stream.IOResult
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import dev.jyuch.alpakka.mybatis.impl.{MyBatisFlowGraphStage, MyBatisSourceGraphStage}
import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.SqlSession

import scala.concurrent.Future

object MyBatis {

  def source[Out](
    sessionFactory: () => SqlSession,
    cursorFactory: SqlSession => Cursor[Out]
  ): Source[Out, Future[IOResult]] = {
    Source.fromGraph(new MyBatisSourceGraphStage[Out](sessionFactory, cursorFactory))
  }

  def flow[In, Out](
    sessionFactory: () => SqlSession,
    action: (SqlSession, In) => Out
  ): Flow[In, Out, Future[IOResult]] = {
    Flow.fromGraph(new MyBatisFlowGraphStage[In, Out](sessionFactory, action))
  }

  def sink[In](
    sessionFactory: () => SqlSession,
    action: (SqlSession, In) => Unit
  ): Sink[In, Future[IOResult]] = {
    Flow[In].viaMat(flow(sessionFactory, action))(Keep.right).toMat(Sink.ignore)(Keep.left)
  }

}
