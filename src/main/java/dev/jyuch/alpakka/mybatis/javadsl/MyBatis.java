package dev.jyuch.alpakka.mybatis.javadsl;

import akka.stream.IOResult;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import dev.jyuch.alpakka.mybatis.compat.FunctionCompat;
import dev.jyuch.alpakka.mybatis.impl.MyBatisFlowGraphStage;
import dev.jyuch.alpakka.mybatis.impl.MyBatisSinkGraphStage;
import dev.jyuch.alpakka.mybatis.impl.MyBatisSourceGraphStage;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.session.SqlSession;
import scala.concurrent.Future;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MyBatis {

    public static <Out> Source<Out, Future<IOResult>> source(
            Supplier<SqlSession> sessionFactory,
            Function<SqlSession, Cursor<Out>> cursorFactory
    ) {
        return Source.fromGraph(
                new MyBatisSourceGraphStage<>(
                        FunctionCompat.supplierToFunction0(sessionFactory),
                        FunctionCompat.functionToFunction1((cursorFactory))
                )
        );
    }

    public static <In, Out> Flow<In, Out, Future<IOResult>> flow(
            Supplier<SqlSession> sessionFactory,
            BiFunction<SqlSession, In, Out> action
    ) {
        return Flow.fromGraph(
                new MyBatisFlowGraphStage<>(
                        FunctionCompat.supplierToFunction0(sessionFactory),
                        FunctionCompat.biFunctionToFunction2(action)
                )
        );
    }

    public static <In> Sink<In, Future<IOResult>> sink(
            Supplier<SqlSession> sessionFactory,
            Function<SqlSession, Function<In, Object>> operationFactory,
            boolean commitEachItem
    ) {
        return Sink.fromGraph(new MyBatisSinkGraphStage<>(
                FunctionCompat.supplierToFunction0(sessionFactory),
                FunctionCompat.nestedFunctionToFunction1(operationFactory),
                commitEachItem
        ));
    }
}
