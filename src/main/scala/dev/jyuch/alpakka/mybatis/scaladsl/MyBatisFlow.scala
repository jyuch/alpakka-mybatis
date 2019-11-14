package dev.jyuch.alpakka.mybatis.scaladsl

import akka.stream.IOResult
import akka.stream.scaladsl.Flow
import dev.jyuch.alpakka.mybatis.impl.MyBatisFlowGraphStage
import org.apache.ibatis.session.SqlSession

import scala.concurrent.Future

object MyBatisFlow {
  def fromSessionFactory[In, Out](
    sessionFactory: () => SqlSession,
    action: (SqlSession, In) => Out
  ): Flow[In, Out, Future[IOResult]] = {
    Flow.fromGraph(new MyBatisFlowGraphStage[In, Out](sessionFactory, action))
  }
}
