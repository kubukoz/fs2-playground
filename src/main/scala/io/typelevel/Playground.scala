package io.typelevel
import cats.effect._
import cats.implicits._
import fs2.concurrent.InspectableQueue
import fs2.Stream

import scala.language.higherKinds
import scala.concurrent.duration._

object Playground extends IOApp {

  private def putStrLn[F[_]: Sync](msg: String): F[Unit] = {
    Sync[F].delay(println(msg))
  }

  val hangingQueue: IO[Unit] = {
    val sizeLimit = 5
    for {
      q <- InspectableQueue.bounded[IO, Unit](sizeLimit)

      job = (jobId: Int) => q.enqueue1(()) *> putStrLn[IO](show"$jobId: offered to queue")

      //enqueue elements concurrently - this should eventually make the queue full
      _ <- (1 to sizeLimit).toList.parTraverse_(job).start

      //show the latest queue's size after changes
      _ <- q.size.discrete.evalMap(size => putStrLn[IO](show"queue size: $size")).compile.drain.start

      //show the queue's size every second
      _ <- Stream.fixedRate[IO](1.second).evalMap(_ => q.size.get).map(_.show).evalMap(putStrLn[IO]).compile.drain.start

      //terminate the program when the queue is full
      _ <- q.full.discrete.takeWhile(!_).compile.drain
    } yield ()
  }

  override def run(args: List[String]): IO[ExitCode] =
    hangingQueue
      .as(ExitCode.Success)
}
