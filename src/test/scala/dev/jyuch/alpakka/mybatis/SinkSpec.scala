package dev.jyuch.alpakka.mybatis

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import dev.jyuch.alpakka.mybatis.model.User
import dev.jyuch.alpakka.mybatis.scaladsl.MyBatis
import dev.jyuch.alpakka.mybatis.service.UserMapper
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.{SqlSession, SqlSessionFactory, SqlSessionFactoryBuilder}
import org.scalatest.{BeforeAndAfter, FlatSpec}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

class SinkSpec extends FlatSpec with BeforeAndAfter {

  implicit var system: ActorSystem = _
  var sqlSessionFactory: SqlSessionFactory = _
  var sessionHolder: SqlSession = _

  before {
    system = ActorSystem("SinkSpec")
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

  "Sink" should "insert item from upstream to db" in {
    val source = Source(collection.immutable.Seq(new User(3, "carol")))
    val sink = MyBatis.sink[User](
      () => sqlSessionFactory.openSession(),
      (session, it) => {
        session.getMapper(classOf[UserMapper]).insert(it)
        session.commit()
      }
    )
    val future = source.toMat(sink)(Keep.right).run()
    val count = Await.result(future, 10 second)

    val cursor = sessionHolder.getMapper(classOf[UserMapper]).select().asScala
    val result = mutable.ListBuffer.empty[User]
    for (it <- cursor) {
      result += it
    }
    assert(result == Seq(new User(1, "alice"), new User(2, "bob"), new User(3, "carol")))
    assert(count.count == 1)
  }
}
