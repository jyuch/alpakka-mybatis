package dev.jyuch.alpakka.mybatis.scaladsl

import akka.NotUsed
import akka.stream.scaladsl.Source
import dev.jyuch.alpakka.mybatis.impl.MyBatisSourceGraphStage
import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.SqlSession

object MyBatisSource {
  def fromSessionFactory[T](
    sessionFactory: () => SqlSession,
    cursorFactory: SqlSession => Cursor[T]
  ): Source[T, NotUsed] = {
    Source.fromGraph(new MyBatisSourceGraphStage[T](sessionFactory, cursorFactory))
  }
}
