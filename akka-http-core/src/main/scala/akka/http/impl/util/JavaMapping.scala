/**
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.http.impl.util

import java.net.InetAddress
import java.{ util ⇒ ju, lang ⇒ jl }
import scala.collection.immutable
import scala.reflect.ClassTag
import akka.japi
import akka.http.impl.model.JavaUri
import akka.http.javadsl.model.Accessors
import akka.http.javadsl.{ model ⇒ jm }
import akka.http.scaladsl.{ model ⇒ sm }

/** INTERNAL API */
trait J2SMapping[J] {
  type S
  def toScala(javaObject: J): S
}
/** INTERNAL API */
object J2SMapping {
  implicit def fromJavaMapping[J](implicit mapping: JavaMapping[J, _]): J2SMapping[J] { type S = mapping.S } = mapping

  implicit def seqMapping[J](implicit mapping: J2SMapping[J]): J2SMapping[Seq[J]] { type S = immutable.Seq[mapping.S] } =
    new J2SMapping[Seq[J]] {
      type S = immutable.Seq[mapping.S]
      def toScala(javaObject: Seq[J]): S = javaObject.map(mapping.toScala(_)).toList
    }
}
/** INTERNAL API */
trait S2JMapping[S] {
  type J
  def toJava(scalaObject: S): J
}
/** INTERNAL API */
object S2JMapping {
  implicit def fromJavaMapping[S](implicit mapping: JavaMapping[_, S]): S2JMapping[S] { type J = mapping.J } = mapping
}

/** INTERNAL API */
trait JavaMapping[_J, _S] extends J2SMapping[_J] with S2JMapping[_S] {
  type J = _J
  type S = _S
}
/** INTERNAL API */
object JavaMapping {
  trait AsScala[S] {
    def asScala: S
  }
  trait AsJava[J] {
    def asJava: J
  }

  object Implicits {
    import scala.language.implicitConversions

    implicit def convertToScala[J](j: J)(implicit mapping: J2SMapping[J]): mapping.S = mapping.toScala(j)
    implicit def convertSeqToScala[J](j: Seq[J])(implicit mapping: J2SMapping[J]): immutable.Seq[mapping.S] =
      j.map(mapping.toScala(_)).toList

    implicit def AddAsScala[J](javaObject: J)(implicit mapping: J2SMapping[J]): AsScala[mapping.S] = new AsScala[mapping.S] {
      def asScala = convertToScala(javaObject)
    }
    implicit def AddAsJava[S](scalaObject: S)(implicit mapping: S2JMapping[S]): AsJava[mapping.J] = new AsJava[mapping.J] {
      def asJava = mapping.toJava(scalaObject)
    }
  }

  implicit def iterableMapping[_J, _S](implicit mapping: JavaMapping[_J, _S]): JavaMapping[jl.Iterable[_J], immutable.Seq[_S]] =
    new JavaMapping[jl.Iterable[_J], immutable.Seq[_S]] {
      import collection.JavaConverters._

      def toJava(scalaObject: immutable.Seq[_S]): jl.Iterable[_J] = scalaObject.map(mapping.toJava(_)).asJavaCollection
      def toScala(javaObject: jl.Iterable[_J]): immutable.Seq[_S] =
        Implicits.convertSeqToScala(iterableAsScalaIterableConverter(javaObject).asScala.toSeq)
    }
  implicit def map[K, V]: JavaMapping[ju.Map[K, V], immutable.Map[K, V]] =
    new JavaMapping[ju.Map[K, V], immutable.Map[K, V]] {
      import scala.collection.JavaConverters._
      def toScala(javaObject: ju.Map[K, V]): immutable.Map[K, V] = javaObject.asScala.toMap
      def toJava(scalaObject: immutable.Map[K, V]): ju.Map[K, V] = scalaObject.asJava
    }
  implicit def option[_J, _S](implicit mapping: JavaMapping[_J, _S]): JavaMapping[akka.japi.Option[_J], Option[_S]] =
    new JavaMapping[akka.japi.Option[_J], Option[_S]] {
      def toScala(javaObject: japi.Option[_J]): Option[_S] = javaObject.asScala.map(mapping.toScala(_))
      def toJava(scalaObject: Option[_S]): japi.Option[_J] = japi.Option.fromScalaOption(scalaObject.map(mapping.toJava(_)))
    }

  implicit object StringIdentity extends Identity[String]

  implicit object LongMapping extends JavaMapping[jl.Long, Long] {
    def toScala(javaObject: jl.Long): Long = javaObject
    def toJava(scalaObject: Long): jl.Long = scalaObject
  }
  implicit object InetAddressIdentity extends Identity[InetAddress]

  class Identity[T] extends JavaMapping[T, T] {
    def toScala(javaObject: T): T = javaObject
    def toJava(scalaObject: T): T = scalaObject
  }
  class Inherited[J <: AnyRef, S <: J](implicit classTag: ClassTag[S]) extends JavaMapping[J, S] {
    def toJava(scalaObject: S): J = scalaObject
    def toScala(javaObject: J): S = cast[S](javaObject)
  }

  implicit object DateTime extends Inherited[jm.DateTime, akka.http.scaladsl.model.DateTime]

  implicit object ContentType extends Inherited[jm.ContentType, sm.ContentType]
  implicit object Host extends Inherited[jm.Host, sm.Uri.Host]
  implicit object HttpCharset extends Inherited[jm.HttpCharset, sm.HttpCharset]
  implicit object HttpCharsetRange extends Inherited[jm.HttpCharsetRange, sm.HttpCharsetRange]
  implicit object HttpEntity extends Inherited[jm.HttpEntity, sm.HttpEntity]
  implicit object HttpHeader extends Inherited[jm.HttpHeader, sm.HttpHeader]
  implicit object HttpMethod extends Inherited[jm.HttpMethod, sm.HttpMethod]
  implicit object HttpProtocol extends Inherited[jm.HttpProtocol, sm.HttpProtocol]
  implicit object HttpRequest extends Inherited[jm.HttpRequest, sm.HttpRequest]
  implicit object HttpResponse extends Inherited[jm.HttpResponse, sm.HttpResponse]
  implicit object MediaRange extends Inherited[jm.MediaRange, sm.MediaRange]
  implicit object MediaType extends Inherited[jm.MediaType, sm.MediaType]
  implicit object StatusCode extends Inherited[jm.StatusCode, sm.StatusCode]

  implicit object ContentRange extends Inherited[jm.ContentRange, sm.ContentRange]
  implicit object RemoteAddress extends Inherited[jm.RemoteAddress, sm.RemoteAddress]
  implicit object TransferEncoding extends Inherited[jm.TransferEncoding, sm.TransferEncoding]

  implicit object ByteRange extends Inherited[jm.headers.ByteRange, sm.headers.ByteRange]
  implicit object CacheDirective extends Inherited[jm.headers.CacheDirective, sm.headers.CacheDirective]
  implicit object ContentDispositionType extends Inherited[jm.headers.ContentDispositionType, sm.headers.ContentDispositionType]
  implicit object EntityTag extends Inherited[jm.headers.EntityTag, sm.headers.EntityTag]
  implicit object EntityTagRange extends Inherited[jm.headers.EntityTagRange, sm.headers.EntityTagRange]
  implicit object HttpChallenge extends Inherited[jm.headers.HttpChallenge, sm.headers.HttpChallenge]
  implicit object HttpCookie extends Inherited[jm.headers.HttpCookie, sm.headers.HttpCookie]
  implicit object HttpCredentials extends Inherited[jm.headers.HttpCredentials, sm.headers.HttpCredentials]
  implicit object HttpEncoding extends Inherited[jm.headers.HttpEncoding, sm.headers.HttpEncoding]
  implicit object HttpEncodingRange extends Inherited[jm.headers.HttpEncodingRange, sm.headers.HttpEncodingRange]
  implicit object HttpOrigin extends Inherited[jm.headers.HttpOrigin, sm.headers.HttpOrigin]
  implicit object HttpOriginRange extends Inherited[jm.headers.HttpOriginRange, sm.headers.HttpOriginRange]
  implicit object Language extends Inherited[jm.headers.Language, sm.headers.Language]
  implicit object LanguageRange extends Inherited[jm.headers.LanguageRange, sm.headers.LanguageRange]
  implicit object LinkParam extends Inherited[jm.headers.LinkParam, sm.headers.LinkParam]
  implicit object LinkValue extends Inherited[jm.headers.LinkValue, sm.headers.LinkValue]
  implicit object ProductVersion extends Inherited[jm.headers.ProductVersion, sm.headers.ProductVersion]
  implicit object RangeUnit extends Inherited[jm.headers.RangeUnit, sm.headers.RangeUnit]

  implicit object Uri extends JavaMapping[jm.Uri, sm.Uri] {
    def toScala(javaObject: jm.Uri): Uri.S = cast[JavaUri](javaObject).uri
    def toJava(scalaObject: sm.Uri): Uri.J = Accessors.Uri(scalaObject)
  }

  private def cast[T](obj: AnyRef)(implicit classTag: ClassTag[T]): T =
    try classTag.runtimeClass.cast(obj).asInstanceOf[T]
    catch {
      case exp: ClassCastException ⇒
        throw new IllegalArgumentException(s"Illegal custom subclass of $classTag. " +
          s"Please use only the provided factories in akka.http.javadsl.model.Http")
    }
}