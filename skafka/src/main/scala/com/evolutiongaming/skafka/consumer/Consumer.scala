package com.evolutiongaming.skafka
package consumer

import java.lang.{Long => LongJ}
import java.util.regex.Pattern
import java.util.{Map => MapJ, Set => SetJ, List => ListJ, Collection => CollectionJ}

import cats.data.{NonEmptyMap => Nem, NonEmptySet => Nes}
import cats.effect._
import cats.effect.concurrent.Semaphore
import cats.effect.implicits._
import cats.implicits._
import cats.{Applicative, Monad, MonadError, ~>}
import com.evolutiongaming.catshelper.{Log, ToFuture, ToTry}
import com.evolutiongaming.skafka.Converters._
import com.evolutiongaming.skafka.consumer.ConsumerConverters._
import com.evolutiongaming.smetrics.MeasureDuration
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener
import org.apache.kafka.clients.consumer.{
  OffsetCommitCallback, Consumer => ConsumerJ,
  OffsetAndMetadata => OffsetAndMetadataJ,
  OffsetAndTimestamp => OffsetAndTimestampJ
}
import org.apache.kafka.common.{TopicPartition => TopicPartitionJ, PartitionInfo => PartitionInfoJ}

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * See [[org.apache.kafka.clients.consumer.Consumer]]
  */
trait Consumer[F[_], K, V] {

  def assign(partitions: Nes[TopicPartition]): F[Unit]

  def assignment: F[Set[TopicPartition]]


  def subscribe(topics: Nes[Topic], listener: Option[RebalanceListener[F]]): F[Unit]

  def subscribe(pattern: Pattern, listener: Option[RebalanceListener[F]]): F[Unit]

  def subscription: F[Set[Topic]]

  def unsubscribe: F[Unit]


  def poll(timeout: FiniteDuration): F[ConsumerRecords[K, V]]


  def commit: F[Unit]

  def commit(timeout: FiniteDuration): F[Unit]

  def commit(offsets: Nem[TopicPartition, OffsetAndMetadata]): F[Unit]

  def commit(offsets: Nem[TopicPartition, OffsetAndMetadata], timeout: FiniteDuration): F[Unit]


  def commitLater: F[Map[TopicPartition, OffsetAndMetadata]]

  def commitLater(offsets: Nem[TopicPartition, OffsetAndMetadata]): F[Unit]


  def seek(partition: TopicPartition, offset: Offset): F[Unit]

  def seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata): F[Unit]
  

  def seekToBeginning(partitions: Nes[TopicPartition]): F[Unit]

  def seekToEnd(partitions: Nes[TopicPartition]): F[Unit]


  def position(partition: TopicPartition): F[Offset]

  def position(partition: TopicPartition, timeout: FiniteDuration): F[Offset]


  def committed(partitions: Nes[TopicPartition]): F[Map[TopicPartition, OffsetAndMetadata]]

  def committed(partitions: Nes[TopicPartition], timeout: FiniteDuration): F[Map[TopicPartition, OffsetAndMetadata]]


  def partitions(topic: Topic): F[List[PartitionInfo]]

  def partitions(topic: Topic, timeout: FiniteDuration): F[List[PartitionInfo]]


  def topics: F[Map[Topic, List[PartitionInfo]]]

  def topics(timeout: FiniteDuration): F[Map[Topic, List[PartitionInfo]]]

  @deprecated("use topics instead", "7.2.0")
  final def listTopics: F[Map[Topic, List[PartitionInfo]]] = topics

  @deprecated("use topics instead", "7.2.0")
  final def listTopics(timeout: FiniteDuration): F[Map[Topic, List[PartitionInfo]]] = topics(timeout)


  def pause(partitions: Nes[TopicPartition]): F[Unit]

  def paused: F[Set[TopicPartition]]

  def resume(partitions: Nes[TopicPartition]): F[Unit]


  def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset]): F[Map[TopicPartition, Option[OffsetAndTimestamp]]]

  def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset], timeout: FiniteDuration): F[Map[TopicPartition, Option[OffsetAndTimestamp]]]


  def beginningOffsets(partitions: Nes[TopicPartition]): F[Map[TopicPartition, Offset]]

  def beginningOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration): F[Map[TopicPartition, Offset]]


  def endOffsets(partitions: Nes[TopicPartition]): F[Map[TopicPartition, Offset]]

  def endOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration): F[Map[TopicPartition, Offset]]

  def wakeup: F[Unit]
}


object Consumer {

  def empty[F[_] : Applicative, K, V]: Consumer[F, K, V] = {

    val empty = ().pure[F]

    new Consumer[F, K, V] {

      def assign(partitions: Nes[TopicPartition]) = empty

      val assignment = Set.empty[TopicPartition].pure[F]

      def subscribe(topics: Nes[Topic], listener: Option[RebalanceListener[F]]) = empty

      def subscribe(pattern: Pattern, listener: Option[RebalanceListener[F]]) = empty

      val subscription: F[Set[Topic]] = Set.empty[Topic].pure[F]

      val unsubscribe = empty

      def poll(timeout: FiniteDuration) = ConsumerRecords.empty[K, V].pure[F]

      val commit = empty

      def commit(timeout: FiniteDuration) = empty

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata]) = empty

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata], timeout: FiniteDuration) = empty

      val commitLater = Map.empty[TopicPartition, OffsetAndMetadata].pure[F]

      def commitLater(offsets: Nem[TopicPartition, OffsetAndMetadata]) = empty

      def seek(partition: TopicPartition, offset: Offset) = empty

      def seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata) = empty

      def seekToBeginning(partitions: Nes[TopicPartition]) = empty

      def seekToEnd(partitions: Nes[TopicPartition]) = empty

      def position(partition: TopicPartition) = Offset.min.pure[F]

      def position(partition: TopicPartition, timeout: FiniteDuration) = Offset.min.pure[F]

      def committed(partitions: Nes[TopicPartition]) = {
        Map.empty[TopicPartition, OffsetAndMetadata].pure[F]
      }

      def committed(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        Map.empty[TopicPartition, OffsetAndMetadata].pure[F]
      }

      def partitions(topic: Topic) = List.empty[PartitionInfo].pure[F]

      def partitions(topic: Topic, timeout: FiniteDuration) = List.empty[PartitionInfo].pure[F]

      val topics = Map.empty[Topic, List[PartitionInfo]].pure[F]

      def topics(timeout: FiniteDuration) = Map.empty[Topic, List[PartitionInfo]].pure[F]

      def pause(partitions: Nes[TopicPartition]) = empty

      val paused = Set.empty[TopicPartition].pure[F]

      def resume(partitions: Nes[TopicPartition]) = empty

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset]) = {
        Map.empty[TopicPartition, Option[OffsetAndTimestamp]].pure[F]
      }

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset], timeout: FiniteDuration) = {
        Map.empty[TopicPartition, Option[OffsetAndTimestamp]].pure[F]
      }

      def beginningOffsets(partitions: Nes[TopicPartition]) = {
        Map.empty[TopicPartition, Offset].pure[F]
      }

      def beginningOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        Map.empty[TopicPartition, Offset].pure[F]
      }

      def endOffsets(partitions: Nes[TopicPartition]) = {
        Map.empty[TopicPartition, Offset].pure[F]
      }

      def endOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        Map.empty[TopicPartition, Offset].pure[F]
      }

      val wakeup = empty
    }
  }


  def of[F[_] : Concurrent : ContextShift : ToTry : ToFuture, K, V](
    config: ConsumerConfig,
    executorBlocking: ExecutionContext)(implicit
    fromBytesK: FromBytes[F, K],
    fromBytesV: FromBytes[F, V]
  ): Resource[F, Consumer[F, K, V]] = {

    val blocking = Blocking(executorBlocking)
    val consumerJ = CreateConsumerJ(config, fromBytesK, fromBytesV, blocking)

    val result = for {
      consumerJ <- consumerJ
      consumer   = apply(consumerJ, blocking)
      release    = blocking { consumerJ.close() }
    } yield {
      (consumer, release)
    }
    serial(Resource(result))
  }


  def serial[F[_] : Concurrent, K, V](consumer: Resource[F, Consumer[F, K, V]]): Resource[F, Consumer[F, K, V]] = {
    val result = for {
      semaphore <- Semaphore[F](1)
      result    <- consumer.allocated
    } yield {
      val (consumer, close) = result

      val serial = new (F ~> F) {
        def apply[A](fa: F[A]) = semaphore.withPermit(fa).uncancelable
      }

      val close1 = serial(close)

      val consumer1 = consumer.mapK(serial, serial)

      (consumer1, close1)
    }
    Resource(result)
  }


  def apply[F[_] : Concurrent : ContextShift : ToFuture, K, V](
    consumer: ConsumerJ[K, V],
    blocking: Blocking[F]
  ): Consumer[F, K, V] = {

    def commitLater1(f: OffsetCommitCallback => Unit) = {
      val commitLater = Async[F].asyncF[MapJ[TopicPartitionJ, OffsetAndMetadataJ]] { f1 =>
        val callback = new OffsetCommitCallback {

          def onComplete(offsets: MapJ[TopicPartitionJ, OffsetAndMetadataJ], exception: Exception) = {
            if (exception != null) {
              f1(exception.asLeft)
            } else if (offsets != null) {
              f1(offsets.asRight)
            } else {
              val failure = SkafkaError("both offsets & exception are nulls")
              f1(failure.asLeft)
            }
          }
        }

        blocking { f(callback) }
      }

      for {
        result <- commitLater
        _      <- ContextShift[F].shift
      } yield result
    }

    def listenerOf(listener: Option[RebalanceListener[F]]) = {
      listener
        .map { _.asJava }
        .getOrElse(new NoOpConsumerRebalanceListener)
    }

    def position1(partition: TopicPartition)(f: TopicPartitionJ => Long) = {
      val partitionJ = partition.asJava
      for {
        offset <- blocking { f(partitionJ) }
        offset <- Offset.of[F](offset)
      } yield offset
    }

    def committed1(
      partitions: Nes[TopicPartition])(
      f: (SetJ[TopicPartitionJ]) => MapJ[TopicPartitionJ, OffsetAndMetadataJ]
    ) = {
      val partitionsJ = partitions.asJava
      for {
        result <- blocking { f(partitionsJ) }
        result <- Option(result).fold {
          Map.empty[TopicPartition, OffsetAndMetadata].pure[F]
        } {
          _.asScalaMap(_.asScala[F], _.asScala[F])
        }
      } yield result
    }

    def partitions1(f: => ListJ[PartitionInfoJ]) = {
      for {
        result <- blocking { f }
        result <- result.asScala.toList.traverse { _.asScala[F] }
      } yield result
    }

    def topics1(f: => MapJ[Topic, ListJ[PartitionInfoJ]]) = {
      for {
        result <- blocking { f }
        result <- result.asScalaMap(_.pure[F], _.asScala.toList.traverse { _.asScala[F] })
      } yield result
    }

    def offsetsForTimes1(
      timestamps: Map[TopicPartition, Offset])(
      f: MapJ[TopicPartitionJ, LongJ] => MapJ[TopicPartitionJ, OffsetAndTimestampJ]
    ) = {
      val timestampsJ = timestamps.asJavaMap(_.asJava, a => LongJ.valueOf(a.value))
      for {
        result <- blocking { f(timestampsJ) }
        result <- result.asScalaMap(_.asScala[F], v => Option(v).traverse { _.asScala[F] })
      } yield result
    }

    def offsetsOf(
      partitions: Nes[TopicPartition])(
      f: CollectionJ[TopicPartitionJ] => MapJ[TopicPartitionJ, LongJ]
    ) = {
      val partitionsJ = partitions.asJava
      for {
        result <- blocking { f(partitionsJ) }
        result <- result.asScalaMap(_.asScala[F], a => Offset.of[F](a))
      } yield result
    }

    new Consumer[F, K, V] {

      def assign(partitions: Nes[TopicPartition]) = {
        val partitionsJ = partitions.toList.map { _.asJava }.asJavaCollection
        Sync[F].delay { consumer.assign(partitionsJ) }
      }

      val assignment = {
        for {
          result <- Sync[F].delay { consumer.assignment() }
          result <- result.asScala.toList.traverse { _.asScala[F] }
        } yield {
          result.toSet
        }
      }

      def subscribe(topics: Nes[Topic], listener: Option[RebalanceListener[F]]) = {
        val topicsJ = topics.toSortedSet.toSet.asJava
        Sync[F].delay { consumer.subscribe(topicsJ, listenerOf(listener)) }
      }

      def subscribe(pattern: Pattern, listener: Option[RebalanceListener[F]]) = {
        Sync[F].delay { consumer.subscribe(pattern, listenerOf(listener)) }
      }

      val subscription = {
        for {
          result <- Sync[F].delay { consumer.subscription() }
        } yield {
          result.asScala.toSet
        }
      }

      val unsubscribe = {
        blocking { consumer.unsubscribe() }
      }

      def poll(timeout: FiniteDuration) = {
        val timeoutJ = timeout.asJava
        for {
          result <- blocking { consumer.poll(timeoutJ) }
          result <- result.asScala[F]
        } yield result
      }

      val commit = {
        blocking { consumer.commitSync() }
      }

      def commit(timeout: FiniteDuration) = {
        val timeoutJ = timeout.asJava
        blocking { consumer.commitSync(timeoutJ) }
      }

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata]) = {
        val offsetsJ = offsets
          .toSortedMap
          .asJavaMap(_.asJava, _.asJava)
        blocking { consumer.commitSync(offsetsJ) }
      }

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata], timeout: FiniteDuration) = {
        val offsetsJ = offsets
          .toSortedMap
          .asJavaMap(_.asJava, _.asJava)
        val timeoutJ = timeout.asJava
        blocking { consumer.commitSync(offsetsJ, timeoutJ) }
      }

      val commitLater = {
        for {
          result <- commitLater1 { callback => consumer.commitAsync(callback) }
          result <- result.asScalaMap(_.asScala[F], _.asScala[F])
        } yield result
      }

      def commitLater(offsets: Nem[TopicPartition, OffsetAndMetadata]) = {
        val offsetsJ = offsets.toSortedMap.deepAsJava
        commitLater1 { callback => consumer.commitAsync(offsetsJ, callback) }.void
      }

      def seek(partition: TopicPartition, offset: Offset) = {
        val partitionsJ = partition.asJava
        Sync[F].delay { consumer.seek(partitionsJ, offset.value) }
      }

      def seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata) = {
        val partitionsJ = partition.asJava
        val offsetAndMetadataJ = offsetAndMetadata.asJava
        Sync[F].delay { consumer.seek(partitionsJ, offsetAndMetadataJ) }
      }

      def seekToBeginning(partitions: Nes[TopicPartition]) = {
        val partitionsJ = partitions.asJava
        Sync[F].delay { consumer.seekToBeginning(partitionsJ) }
      }

      def seekToEnd(partitions: Nes[TopicPartition]) = {
        val partitionsJ = partitions.asJava
        Sync[F].delay { consumer.seekToEnd(partitionsJ) }
      }

      def position(partition: TopicPartition) = {
        position1(partition) { consumer.position }
      }


      def position(partition: TopicPartition, timeout: FiniteDuration) = {
        position1(partition) { consumer.position(_, timeout.asJava) }
      }

      def committed(partitions: Nes[TopicPartition]) = {
        committed1(partitions) { consumer.committed }
      }

      def committed(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        committed1(partitions) { consumer.committed(_, timeout.asJava) }
      }

      def partitions(topic: Topic) = {
        partitions1 { consumer.partitionsFor(topic) }
      }

      def partitions(topic: Topic, timeout: FiniteDuration) = {
        partitions1 { consumer.partitionsFor(topic, timeout.asJava) }
      }

      val topics = topics1 { consumer.listTopics() }

      def topics(timeout: FiniteDuration) = {
        topics1 { consumer.listTopics(timeout.asJava) }
      }

      def pause(partitions: Nes[TopicPartition]) = {
        val partitionsJ = partitions.asJava
        Sync[F].delay { consumer.pause(partitionsJ) }
      }

      val paused = {
        for {
          result <- Sync[F].delay { consumer.paused() }
          result <- result.asScala.toList.traverse { _.asScala[F] }
        } yield {
          result.toSet
        }
      }

      def resume(partitions: Nes[TopicPartition]) = {
        val partitionsJ = partitions.asJava
        Sync[F].delay { consumer.resume(partitionsJ) }
      }

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset]) = {
        offsetsForTimes1(timestampsToSearch) { consumer.offsetsForTimes }
      }

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset], timeout: FiniteDuration) = {
        val timeoutJ = timeout.asJava
        offsetsForTimes1(timestampsToSearch) { consumer.offsetsForTimes(_, timeoutJ) }
      }

      def beginningOffsets(partitions: Nes[TopicPartition]) = {
        offsetsOf(partitions) { consumer.beginningOffsets }
      }

      def beginningOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        val timeoutJ = timeout.asJava
        offsetsOf(partitions) { consumer.beginningOffsets(_, timeoutJ) }
      }

      def endOffsets(partitions: Nes[TopicPartition]) = {
        offsetsOf(partitions) { consumer.endOffsets }
      }

      def endOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        val timeoutJ = timeout.asJava
        offsetsOf(partitions) { consumer.endOffsets(_, timeoutJ) }
      }

      val wakeup = {
        blocking { consumer.wakeup() }
      }
    }
  }


  def apply[F[_] : MeasureDuration, E, K, V](
    consumer: Consumer[F, K, V],
    metrics: ConsumerMetrics[F])(implicit
    F: MonadError[F, E]
  ): Consumer[F, K, V] = {

    implicit val monoidUnit = Applicative.monoid[F, Unit]

    val topics = for {
      topicPartitions <- consumer.assignment
    } yield for {
      topicPartition <- topicPartitions
    } yield {
      topicPartition.topic
    }


    def call[A](name: String, topics: Iterable[Topic])(fa: F[A]): F[A] = {
      for {
        d <- MeasureDuration[F].start
        r <- fa.attempt
        d <- d
        _ <- topics.toList.foldMap { topic => metrics.call(name, topic, d, r.isRight) }
        r <- r.liftTo[F]
      } yield r
    }

    def call1[T](name: String)(f: F[T]): F[T] = {
      for {
        topics <- topics
        r      <- call(name, topics)(f)
      } yield r
    }


    def count(name: String, topics: Iterable[Topic]) = {
      topics.toList.foldMapM { topic => metrics.count(name, topic) }
    }

    def count1(name: String): F[Unit] = {
      for {
        topics <- topics
        r      <- count(name, topics)
      } yield r
    }

    def rebalanceListener(listener: RebalanceListener[F]) = {

      def measure(name: String, partitions: Nes[TopicPartition]) = {
        partitions.foldMapM { metrics.rebalance(name, _) }
      }

      new RebalanceListener[F] {

        def onPartitionsAssigned(partitions: Nes[TopicPartition]) = {
          for {
            _ <- measure("assigned", partitions)
            a <- listener.onPartitionsAssigned(partitions)
          } yield a
        }

        def onPartitionsRevoked(partitions: Nes[TopicPartition]) = {
          for {
            _ <- measure("revoked", partitions)
            a <- listener.onPartitionsRevoked(partitions)
          } yield a
        }
      }
    }

    

    new Consumer[F, K, V] {

      def assign(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList.toSet
        for {
          _ <- count("assign", topics)
          r <- consumer.assign(partitions)
        } yield r
      }

      val assignment = consumer.assignment

      def subscribe(topics: Nes[Topic], listener: Option[RebalanceListener[F]]) = {
        val listener1 = listener.map(rebalanceListener)
        for {
          _ <- count("subscribe", topics.toList)
          r <- consumer.subscribe(topics, listener1)
        } yield r
      }

      def subscribe(pattern: Pattern, listener: Option[RebalanceListener[F]]) = {
        val listener1 = listener.map(rebalanceListener)
        for {
          _ <- count("subscribe", List("pattern"))
          r <- consumer.subscribe(pattern, listener1)
        } yield r
      }

      val subscription = consumer.subscription

      val unsubscribe = {
        call1("unsubscribe") { consumer.unsubscribe }
      }

      def poll(timeout: FiniteDuration) =
        for {
          records <- call1("poll") { consumer.poll(timeout) }
          topics    = records.values.values.flatMap(_.toList).groupBy(_.topic)
          _       <- topics.toList.traverse { case (topic, topicRecords) =>
            val bytes = topicRecords.flatMap(_.value).map(_.serializedSize).sum
            metrics.poll(topic, bytes = bytes, records = topicRecords.size)
          }
        } yield records

      val commit = {
        call1("commit") { consumer.commit }
      }

      def commit(timeout: FiniteDuration) = {
        call1("commit") { consumer.commit(timeout) }
      }

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata]) = {
        val topics = offsets
          .keys
          .toList
          .map { _.topic }
        call("commit", topics) { consumer.commit(offsets) }
      }

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata], timeout: FiniteDuration) = {
        val topics = offsets
          .keys
          .toList
          .map(_.topic)
        call("commit", topics) { consumer.commit(offsets, timeout) }
      }

      val commitLater = call1("commit_later") {
        consumer.commitLater
      }

      def commitLater(offsets: Nem[TopicPartition, OffsetAndMetadata]) = {
        val topics = offsets
          .keys
          .toList
          .map(_.topic)
        call("commit_later", topics) { consumer.commitLater(offsets) }
      }

      def seek(partition: TopicPartition, offset: Offset) = {
        for {
          _ <- count("seek", List(partition.topic))
          r <- consumer.seek(partition, offset)
        } yield r
      }

      def seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata) = {
        for {
          _ <- count("seek", List(partition.topic))
          r <- consumer.seek(partition, offsetAndMetadata)
        } yield r
      }

      def seekToBeginning(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList
        for {
          _ <- count("seek_to_beginning", topics)
          r <- consumer.seekToBeginning(partitions)
        } yield r
      }

      def seekToEnd(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList
        for {
          _ <- count("seek_to_end", topics)
          r <- consumer.seekToEnd(partitions)
        } yield r
      }

      def position(partition: TopicPartition) = {
        for {
          _ <- count("position", List(partition.topic))
          r <- consumer.position(partition)
        } yield r
      }

      def position(partition: TopicPartition, timeout: FiniteDuration) = {
        for {
          _ <- count("position", List(partition.topic))
          r <- consumer.position(partition, timeout)
        } yield r
      }

      def committed(partitions: Nes[TopicPartition]) = {
        def topics = partitions
          .toList
          .map { _.topic }
          .distinct
        for {
          _ <- count("committed", topics)
          r <- consumer.committed(partitions)
        } yield r
      }

      def committed(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        def topics = partitions
          .toList
          .map { _.topic }
          .distinct
        for {
          _ <- count("committed", topics)
          r <- consumer.committed(partitions, timeout)
        } yield r
      }

      def partitions(topic: Topic) = {
        for {
          _ <- count("partitions", List(topic))
          r <- consumer.partitions(topic)
        } yield r
      }

      def partitions(topic: Topic, timeout: FiniteDuration) = {
        for {
          _ <- count("partitions", List(topic))
          r <- consumer.partitions(topic, timeout)
        } yield r
      }

      val topics = {
        for {
          d <- MeasureDuration[F].start
          r <- consumer.topics.attempt
          d <- d
          _ <- metrics.topics(d)
          r <- r.liftTo[F]
        } yield r
      }

      def topics(timeout: FiniteDuration) = {
        for {
          d <- MeasureDuration[F].start
          r <- consumer.topics(timeout).attempt
          d <- d
          _ <- metrics.topics(d)
          r <- r.liftTo[F]
        } yield r
      }

      def pause(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList
        for {
          _ <- count("pause", topics)
          r <- consumer.pause(partitions)
        } yield r
      }

      val paused = consumer.paused

      def resume(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList
        for {
          _ <- count("resume", topics)
          r <- consumer.resume(partitions)
        } yield r
      }

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset]) = {
        val topics = timestampsToSearch.keySet.map(_.topic)
        call("offsets_for_times", topics) { consumer.offsetsForTimes(timestampsToSearch) }
      }

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset], timeout: FiniteDuration) = {
        val topics = timestampsToSearch.keySet.map(_.topic)
        call("offsets_for_times", topics) { consumer.offsetsForTimes(timestampsToSearch, timeout) }
      }

      def beginningOffsets(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList
        call("beginning_offsets", topics) { consumer.beginningOffsets(partitions) }
      }

      def beginningOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        val topics = partitions.map(_.topic).toList
        call("beginning_offsets", topics) { consumer.beginningOffsets(partitions, timeout) }
      }

      def endOffsets(partitions: Nes[TopicPartition]) = {
        val topics = partitions.map(_.topic).toList
        call("end_offsets", topics) { consumer.endOffsets(partitions) }
      }

      def endOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        val topics = partitions.map(_.topic).toList
        call("end_offsets", topics) { consumer.endOffsets(partitions, timeout) }
      }

      val wakeup = {
        for {
          _ <- count1("wakeup")
          r <- consumer.wakeup
        } yield r
      }
    }
  }


  implicit class ConsumerOps[F[_], K, V](val self: Consumer[F, K, V]) extends AnyVal {

    def withMetrics[E](
      metrics: ConsumerMetrics[F])(implicit
      F: MonadError[F, E],
      measureDuration: MeasureDuration[F],
    ): Consumer[F, K, V] = {
      Consumer(self, metrics)
    }


    def withLogging(
      log: Log[F])(implicit
      F: Monad[F],
      measureDuration: MeasureDuration[F]
    ): Consumer[F, K, V] = {
      ConsumerLogging(log, self)
    }


    def mapK[G[_]](fg: F ~> G, gf: G ~> F): Consumer[G, K, V] = new Consumer[G, K, V] {

      def assign(partitions: Nes[TopicPartition]) = fg(self.assign(partitions))

      def assignment = fg(self.assignment)

      def subscribe(topics: Nes[Topic], listener: Option[RebalanceListener[G]]) = {
        val listener1 = listener.map(_.mapK(gf))
        fg(self.subscribe(topics, listener1))
      }

      def subscribe(pattern: Pattern, listener: Option[RebalanceListener[G]]) = {
        val listener1 = listener.map(_.mapK(gf))
        fg(self.subscribe(pattern, listener1))
      }

      def subscription = fg(self.subscription)

      def unsubscribe = fg(self.unsubscribe)

      def poll(timeout: FiniteDuration) = fg(self.poll(timeout))

      def commit = fg(self.commit)

      def commit(timeout: FiniteDuration) = fg(self.commit(timeout))

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata]) = fg(self.commit(offsets))

      def commit(offsets: Nem[TopicPartition, OffsetAndMetadata], timeout: FiniteDuration) = fg(self.commit(offsets, timeout))

      def commitLater = fg(self.commitLater)

      def commitLater(offsets: Nem[TopicPartition, OffsetAndMetadata]) = fg(self.commitLater(offsets))

      def seek(partition: TopicPartition, offset: Offset) = fg(self.seek(partition, offset))

      def seek(partition: TopicPartition, offsetAndMetadata: OffsetAndMetadata) = fg(self.seek(partition, offsetAndMetadata))

      def seekToBeginning(partitions: Nes[TopicPartition]) = fg(self.seekToBeginning(partitions))

      def seekToEnd(partitions: Nes[TopicPartition]) = fg(self.seekToEnd(partitions))

      def position(partition: TopicPartition) = fg(self.position(partition))

      def position(partition: TopicPartition, timeout: FiniteDuration) = fg(self.position(partition, timeout))

      def committed(partitions: Nes[TopicPartition]) = fg(self.committed(partitions))

      def committed(partitions: Nes[TopicPartition], timeout: FiniteDuration) = fg(self.committed(partitions, timeout))

      def partitions(topic: Topic) = fg(self.partitions(topic))

      def partitions(topic: Topic, timeout: FiniteDuration) = fg(self.partitions(topic, timeout))

      def topics = fg(self.topics)

      def topics(timeout: FiniteDuration) = fg(self.topics(timeout))

      def pause(partitions: Nes[TopicPartition]) = fg(self.pause(partitions))

      def paused = fg(self.paused)

      def resume(partitions: Nes[TopicPartition]) = fg(self.resume(partitions))

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset]) = {
        fg(self.offsetsForTimes(timestampsToSearch))
      }

      def offsetsForTimes(timestampsToSearch: Map[TopicPartition, Offset], timeout: FiniteDuration) = {
        fg(self.offsetsForTimes(timestampsToSearch, timeout))
      }

      def beginningOffsets(partitions: Nes[TopicPartition]) = fg(self.beginningOffsets(partitions))

      def beginningOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        fg(self.beginningOffsets(partitions, timeout))
      }

      def endOffsets(partitions: Nes[TopicPartition]) = {
        fg(self.endOffsets(partitions))
      }

      def endOffsets(partitions: Nes[TopicPartition], timeout: FiniteDuration) = {
        fg(self.endOffsets(partitions, timeout))
      }

      def wakeup = fg(self.wakeup)
    }
  }
}

