package dev.jyuch.alpakka.mybatis.scaladsl

import akka.NotUsed
import akka.stream.scaladsl.Flow
import dev.jyuch.alpakka.mybatis.impl.MyBatisFlowGraphStage
import org.apache.ibatis.session.SqlSession

object MyBatisFlow {
  def fromSessionFactory[In, Out](
    sessionFactory: () => SqlSession,
    action: (SqlSession, In) => Out
  ): Flow[In, Out, NotUsed] = {
    Flow.fromGraph(new MyBatisFlowGraphStage[In, Out](sessionFactory, action))
  }
}
