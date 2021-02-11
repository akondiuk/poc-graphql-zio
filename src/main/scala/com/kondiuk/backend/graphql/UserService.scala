package com.kondiuk.backend.graphql

import com.kondiuk.backend.Data._
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates
import zio._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}

object UserService {

  type UserService = Has[Service]

  trait Service {
    def findUser(firstName: String): Task[User] // get request
    def findUsers(): Task[Seq[User]] // get request
    def addUser(user: User): Task[Unit] // post request
    def editUser(user: User): Task[Long] // put request
  }

  def findUser(firstName: String): RIO[UserService, User] =
    RIO.accessM(_.get.findUser(firstName))

  def findUsers(): RIO[UserService, Seq[User]] =
    RIO.accessM(_.get.findUsers())

  def addUser(user: User): RIO[UserService, Unit] =
    RIO.accessM(_.get.addUser(user))

  def editUser(user: User): RIO[UserService, Long] =
    RIO.accessM(_.get.editUser(user))

  def make(userCollection: MongoCollection[User]): ZLayer[Any, Nothing, UserService] = (for {
    userCollection <- Ref.make(userCollection)
  } yield
    new Service {
      implicit val ec: ExecutionContext = ExecutionContext.global

      override def findUser(firstName: String): Task[User] = {
        userCollection.get.flatMap(c =>
          ZIO.fromFuture(
            implicit ec =>
              c.find(equal("firstName", firstName))
                .toFuture()
                .recoverWith { case e => Future.failed(e) }
                .map(_.head)
          )
        )
      }

      override def findUsers(): Task[Seq[User]] = {
        userCollection.get.flatMap(c =>
          ZIO.fromFuture(
            implicit ec =>
              c.find()
                .toFuture()
                .recoverWith { case e => Future.failed(e) }

          )
        )
      }

      override def addUser(user: User): Task[Unit] = {
        userCollection.get.flatMap(c =>
          ZIO.fromFuture(
            implicit ec =>
              c.insertOne(user)
                .toFuture()
                .recoverWith { case e => Future.failed(e) }
                .mapTo
          )
        )

      }

      override def editUser(user: User): Task[Long] = {
        userCollection.get.flatMap(c =>
          ZIO.fromFuture(
            implicit ec =>
              c.updateOne(equal("firstName", user.firstName), Updates.combine(Updates.set("city", user.city), Updates.set("bio", user.bio)))
                .toFuture()
                .recoverWith { case e => Future.failed(e) }
                .map(_.getModifiedCount)
          ))
      }
    }).toLayer

  private var users: Map[String, User] = Map(
    "Peter" -> User("Peter", "Wilson", "12/09/1988", "Programmer", "Austin", "Brave one"),
    "Adam" -> User("Adam", "Norton", "02/04/2001", "Teacher", "Leeds", "Good lad"),
    "Vasilij" -> User("Vasilij", "Petrov", "18/02/1978", "Doctor", "Yalta", "Helping everybody"),
    "Alan" -> User("Alan", "Key", "08/11/1964", "Programmer", "Washington", "Best programmer ever"),
    "Ben" -> User("Ben", "Adams", "01/12/1976", "Dentist", "Austin", "Good dentist"),
    "Anna" -> User("Anna", "Petrova", "04/04/1981", "Designer", "Lublin", "Awesome designer"),
    "Viktor" -> User("Viktor", "Rizhov", "09/05/1969", "Actor", "Voronezh", "Super actor")
  )
  // TODO check it and remove if not needed
  // The real service implementation
  /*   val userService: UserService = new UserService {
       //    override def findUser(id: UserId): IO[UserNotFound, User] =
       override def findUser(firstName: String): Task[User] = {
         val result: Option[User] = users.get(firstName)

         val res: IO[UserNotFound, User] = result match {
           case Some(user) => IO.succeed(user)
           case None => IO.fail(UserNotFound(firstName))
         }

         res
       }

       override def addUser(user: User): UIO[Unit] = ???

       //    override def editUser(user: User): IO[UserNotFound, User] = ???
       override def editUser(user: User): Task[Long] = ???
     }*/

  //   The experimental MongoDB service
  /*   val mongoDBService: UserService = new UserService {
       implicit val ec: ExecutionContext = ExecutionContext.global


      override def findUser(firstName: String): Task[User] = {
         /*  val res = for {
           uc <- setupMongoConfiguration[User](
             uri,
             dbName,
             collectionName
           )
         } yield uc.get.flatMap { c =>
           ZIO.fromFuture(
             implicit ec =>
               c.find().toFuture().recoverWith { case e => Future.failed(e) }
           )
         }
         res */
         ZIO.succeed(User("", "", "", "", "", ""))
       }

       override def addUser(user: User): UIO[Unit] = UIO.unit

       //    override def editUser(user: User): IO[UserNotFound, User] = /*IO.fail(UserNotFound(user.id))*/ ???
       override def editUser(user: User): Task[Long] = /*IO.fail(UserNotFound(user.id))*/ ???
     }*/

  //   The mocked one for Testing - with a simple Map implementation
  val mockedZLayer: ULayer[Has[Service]] = ZLayer.succeed(
    new Service {
      override def findUser(firstName: String): Task[User] = users.get(firstName) match {
        case Some(user) => IO.succeed(user)
        case None => IO.fail(UserNotFound(firstName))
      }

      override def findUsers(): Task[Seq[User]] = ???

      override def addUser(user: User): Task[Unit] = Task.unit

      //    override def editUser(id: UserId): IO[UserNotFound, User] = IO.fail(UserNotFound(id))
      override def editUser(user: User): Task[Long] = /*users.get(user.id) match*/ {
        /*case Some(user) => {
          users.map {
            case (user.id, _) => user.id -> user
            case u => u
          }
          UIO.succeed(user)
        }
        case None => IO.fail(UserNotFound(user.id))*/
        //      case None => UIO.fail(UserNotFound(user.id))
        ???
      }

    })
}
