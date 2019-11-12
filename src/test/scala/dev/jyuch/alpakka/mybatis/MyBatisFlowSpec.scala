package dev.jyuch.alpakka.mybatis

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import dev.jyuch.alpakka.mybatis.model.User
import dev.jyuch.alpakka.mybatis.scaladsl.MyBatisFlow
import dev.jyuch.alpakka.mybatis.service.UserMapper
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory, SqlSessionFactoryBuilder}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.concurrent.duration._
import scala.collection.immutable
import scala.concurrent.Await

class MyBatisFlowSpec extends FlatSpec with BeforeAndAfter {

  implicit var system: ActorSystem = _
  var sqlSessionFactory: SqlSessionFactory = _
  var sessionHolder: SqlSession = _

  before {
    system = ActorSystem("MyBatisFlowSpec")
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

  "MyBatisFlow" should "" in {
    val source = Source(immutable.Seq(1, 2))
    val flow = MyBatisFlow.fromSessionFactory[Int, User](
      () => sqlSessionFactory.openSession(),
      (session, i) => {
        val mapper = session.getMapper(classOf[UserMapper])
        mapper.selectById(i)
      }
    )
    val future = source.via(flow).runWith(Sink.seq)
    val result = Await.result(future, 10 second)
    assert(result == Seq(new User(1, "alice"), new User(2, "bob")))
  }

}
