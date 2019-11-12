package dev.jyuch.alpakka.mybatis.scaladsl

import akka.stream.IOResult
import akka.stream.scaladsl.Sink
import dev.jyuch.alpakka.mybatis.impl.MyBatisSinkGraphStage
import org.apache.ibatis.session.SqlSession

import scala.concurrent.Future

object MyBatisSink {
  def fromSessionFactory[T](
    sessionFactory: () => SqlSession,
    operationFactory: SqlSession => T => Any,
    commitEachItem: Boolean = true
  ): Sink[T, Future[IOResult]] = {
    Sink.fromGraph(new MyBatisSinkGraphStage[T](sessionFactory, operationFactory, commitEachItem))
  }
}
