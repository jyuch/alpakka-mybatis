package dev.jyuch.alpakka.mybatis

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink}
import dev.jyuch.alpakka.mybatis.model._
import dev.jyuch.alpakka.mybatis.scaladsl.MyBatisSource
import dev.jyuch.alpakka.mybatis.service.UserMapper
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory, SqlSessionFactoryBuilder}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class MyBatisSourceSpec extends FlatSpec with BeforeAndAfter {

  implicit var system: ActorSystem = _
  implicit var ec: ExecutionContext = _
  var sqlSessionFactory: SqlSessionFactory = _
  var sessionHolder: SqlSession = _

  before {
    system = ActorSystem("MyBatisSourceSpec")
    ec = system.dispatcher
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsStream("mybatis-config.xml"))
    sessionHolder = sqlSessionFactory.openSession()
    val mapper: UserMapper = sessionHolder.getMapper(classOf[UserMapper])
    mapper.initialize()
    sessionHolder.commit()
  }

  after {
    sessionHolder.close()
    system.terminate()
  }

  "Source" should "return table contents" in {
    val source = MyBatisSource.fromSessionFactory(
      () => sqlSessionFactory.openSession(),
      session => session.getMapper(classOf[UserMapper]).select()
    )
    val (countFuture, resultFuture) = source.toMat(Sink.seq)(Keep.both).run()
    val (count, result) = Await.result(
      for {
        c <- countFuture
        r <- resultFuture
      } yield (c, r), 10 seconds)
    assert(count.count == 2)
    assert(result == Seq(new User(1, "alice"), new User(2, "bob")))
  }

  "Source" should "stop and resource cleanup when downstream is finished" in {
    val source = MyBatisSource.fromSessionFactory(
      () => sqlSessionFactory.openSession(),
      session => session.getMapper(classOf[UserMapper]).select()
    ).take(1)
    val (countFuture, resultFuture) = source.toMat(Sink.seq)(Keep.both).run()
    val (count, result) = Await.result(
      for {
        c <- countFuture
        r <- resultFuture
      } yield (c, r), 10 seconds)
    assert(count.count == 1)
    assert(result == Seq(new User(1, "alice")))
  }
}
