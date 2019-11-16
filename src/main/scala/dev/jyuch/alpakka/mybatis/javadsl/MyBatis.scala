package dev.jyuch.alpakka.mybatis.javadsl

import java.util.concurrent.CompletionStage
import java.util.function.{BiConsumer, BiFunction, Function, Supplier}

import akka.stream.IOResult
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import dev.jyuch.alpakka.mybatis.compat.FunctionCompat
import dev.jyuch.alpakka.mybatis.impl.{MyBatisFlowGraphStage, MyBatisSourceGraphStage}
import org.apache.ibatis.cursor.Cursor
import org.apache.ibatis.session.SqlSession

import scala.compat.java8.FutureConverters._

object MyBatis {

  def source[Out](
    sessionFactory: Supplier[SqlSession],
    cursorFactory: Function[SqlSession, Cursor[Out]]
  ): akka.stream.javadsl.Source[Out, CompletionStage[IOResult]] = {
    Source.fromGraph(new MyBatisSourceGraphStage[Out](
      FunctionCompat.supplierToFunction0(sessionFactory),
      FunctionCompat.functionToFunction1(cursorFactory))).mapMaterializedValue(toJava).asJava
  }

  def flow[In, Out](
    sessionFactory: Supplier[SqlSession],
    action: BiFunction[SqlSession, In, Out]
  ): akka.stream.javadsl.Flow[In, Out, CompletionStage[IOResult]] = {
    Flow.fromGraph(new MyBatisFlowGraphStage[In, Out](
      FunctionCompat.supplierToFunction0(sessionFactory),
      FunctionCompat.biFunctionToFunction2(action))).mapMaterializedValue(toJava).asJava
  }

  def sink[In](
    sessionFactory: Supplier[SqlSession],
    action: BiConsumer[SqlSession, In]
  ): akka.stream.javadsl.Sink[In, CompletionStage[IOResult]] = {
    val flow = Flow.fromGraph(new MyBatisFlowGraphStage[In, Unit](
      FunctionCompat.supplierToFunction0(sessionFactory),
      FunctionCompat.biConsumerToFunction2(action)
    ))
    Flow[In].viaMat(flow)(Keep.right).toMat(Sink.ignore)(Keep.left).mapMaterializedValue(toJava).asJava
  }

}
