package dev.jyuch.alpakka.mybatis.scaladsl

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import dev.jyuch.alpakka.mybatis.impl.MyBatisSourceGraphStage
import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.SqlSession

import scala.concurrent.Future

object MyBatisSource {
  def fromSessionFactory[T](
    sessionFactory: () => SqlSession,
    cursorFactory: SqlSession => Cursor[T]
  ): Source[T, Future[IOResult]] = {
    Source.fromGraph(new MyBatisSourceGraphStage[T](sessionFactory, cursorFactory))
  }
}
