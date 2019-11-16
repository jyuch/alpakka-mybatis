package dev.jyuch.alpakka.mybatis

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}
import dev.jyuch.alpakka.mybatis.scaladsl.MyBatis
import dev.jyuch.alpakka.mybatis.model.User
import dev.jyuch.alpakka.mybatis.service.UserMapper
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory, SqlSessionFactoryBuilder}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.concurrent.duration._
import scala.collection.immutable
import scala.concurrent.{Await, ExecutionContext}

class FlowSpec extends FlatSpec with BeforeAndAfter {

  implicit var system: ActorSystem = _
  implicit var ec: ExecutionContext = _
  var sqlSessionFactory: SqlSessionFactory = _
  var sessionHolder: SqlSession = _

  before {
    system = ActorSystem("FlowSpec")
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

  "Flow" should "process items using mybatis" in {
    val source = Source(immutable.Seq(1, 2))
    val flow = MyBatis.flow[Int, User](
      () => sqlSessionFactory.openSession(),
      (session, i) => {
        val mapper = session.getMapper(classOf[UserMapper])
        mapper.selectById(i)
      }
    )
    val (countFuture, resultFuture) = source.viaMat(flow)(Keep.right).toMat(Sink.seq)(Keep.both).run()
    val (count, result) = Await.result(
      for {
        c <- countFuture
        r <- resultFuture
      } yield (c, r), 10 seconds)
    assert(result == Seq(new User(1, "alice"), new User(2, "bob")))
    assert(count.count == 2)
  }

}
