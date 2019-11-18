package dev.jyuch.alpakka.mybatis;

import akka.stream.IOResult;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import dev.jyuch.alpakka.mybatis.javadsl.MyBatis;

import java.util.concurrent.CompletionStage;

public class EnsureAbleToCompile {
    public static void ensure() {
        Source<Integer, CompletionStage<IOResult>> source = MyBatis.source(() -> null, session -> null);
        Flow<Integer, Integer, CompletionStage<IOResult>> flow = MyBatis.flow(() -> null, (session, x) -> 1, true);
        Sink<Integer, CompletionStage<IOResult>> sink = MyBatis.sink(() -> null, (session, x) -> { }, true);
        source.via(flow).to(sink);
    }
}
