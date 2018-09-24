package io.typelevel
import cats.effect._
import cats.implicits._
import fs2.concurrent.InspectableQueue

import scala.language.higherKinds

object Playground extends IOApp {

  private def putStrLn[F[_]: Sync](msg: String): F[Unit] = {
    Sync[F].delay(println(msg))
  }

  val hangingQueue: IO[Unit] = {
    val sizeLimit = 5
    for {
      q <- InspectableQueue.bounded[IO, Unit](sizeLimit)

      job = (jobId: Int) =>
        (q.enqueue1(()) *> putStrLn[IO](show"$jobId: offered to queue")).start

      _ <- (1 to sizeLimit).toList.traverse(job).map(_.combineAll)
      _ <- q.size.discrete.evalMap(size => putStrLn[IO](show"queue size: $size")).compile.drain.start
      _ <- q.full.discrete.takeWhile(!_).compile.drain
    } yield ()
  }

  override def run(args: List[String]): IO[ExitCode] =
    hangingQueue
      .as(ExitCode.Success)
}
