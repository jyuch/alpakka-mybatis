package dev.jyuch.alpakka.mybatis

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import dev.jyuch.alpakka.mybatis.model._
import dev.jyuch.alpakka.mybatis.scaladsl.MyBatisSource
import dev.jyuch.alpakka.mybatis.service.UserMapper
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory, SqlSessionFactoryBuilder}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class MyBatisSourceSpec extends FlatSpec with BeforeAndAfter {

  implicit var system: ActorSystem = _
  var sqlSessionFactory: SqlSessionFactory = _
  var sessionHolder: SqlSession = _

  before {
    system = ActorSystem("MyBatisSourceSpec")
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

  "MyBatisSource" should "return table contents" in {
    val source = MyBatisSource.fromSessionFactory(
      () => sqlSessionFactory.openSession(),
      session => session.getMapper(classOf[UserMapper]).select()
    )
    val future = source.runWith(Sink.seq)
    val result = Await.result(future, 10 seconds)
    assert(result == Seq(new User(1, "alice"), new User(2, "bob")))
  }

  "MyBatisSource" should "stop and resource cleanup when downstream is finished" in {
    val source = MyBatisSource.fromSessionFactory(
      () => sqlSessionFactory.openSession(),
      session => session.getMapper(classOf[UserMapper]).select()
    ).take(1)
    val future = source.runWith(Sink.seq)
    val result = Await.result(future, 10 seconds)
    assert(result == Seq(new User(1, "alice")))
  }

}
