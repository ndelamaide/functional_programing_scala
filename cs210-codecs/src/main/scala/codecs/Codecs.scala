package codecs

/**
 * A data type modeling JSON values.
 *
 * For example, the `42` integer JSON value can be modeled as `Json.Num(42)`
 */
enum Json:
  /** The JSON `null` value */
  case Null
  /** JSON boolean values */
  case Bool(value: Boolean)
  /** JSON numeric values */
  case Num(value: BigDecimal)
  /** JSON string values */
  case Str(value: String)
  /** JSON objects */
  case Obj(fields: Map[String, Json])
  /** JSON arrays */
  case Arr(items: List[Json])

  /**
   * Try to decode this JSON value into a value of type `A` using `decoder`.
   *
   * Note that you have to explicitly specify the type parameter `A`  when calling this method:
   *
   *     someJsonValue.decodeAs[User] // OK
   *     someJsonValue.decodeAs       // Wrong!
   *
   */
  def decodeAs[A](using decoder: Decoder[A]): Option[A] =
    decoder.decode(this)

end Json

/**
 * A type class that turns a value of type `A` into its JSON representation.
 */
trait Encoder[-A]:

  /** Encodes a value of type `A` into JSON */
  def encode(value: A): Json

  /**
   * Transform this `Encoder[A]` into an `Encoder[B]`, given a transformation function
   * from `B` to `A`.
   *
   * For instance, given an `Encoder[String]`, we can get an `Encoder[UUID]`:
   *
   *     def uuidEncoder(given stringEncoder: Encoder[String]): Encoder[UUID] =
   *       stringEncoder.transform[UUID](uuid => uuid.toString)
   *
   * This operation is also known as "contramap".
   */
  def transform[B](f: B => A): Encoder[B] =
    val originalEncoder = this

    new Encoder[B]:
      def encode(value: B): Json =
        originalEncoder.encode(f(value))
  end transform

end Encoder

object Encoder extends EncoderInstances

trait EncoderInstances:

  /** An encoder for the `Unit` value */
  given Encoder[Unit]:
    def encode(value: Unit): Json =
      Json.Null

  /** An encoder for `Int` values */
  given Encoder[Int]:
    def encode(value: Int): Json =
      Json.Num(BigDecimal(value))

  /** An encoder for `String` values */
  given Encoder[String]:
    def encode(value: String): Json =
      Json.Str(String(value)) // TODO Implement the `Encoder[String]` instance

  /** An encoder for `Boolean` values */
  // TODO Define an anonymous given instance of type `Encoder[Boolean]`
  given Encoder[Boolean]:
    def encode(value: Boolean): Json =
      Json.Bool(value)

  /**
   * Encodes a list of values of type `A` into a JSON array containing
   * the list elements encoded with the given `elemEncoder`
   */
  given listEncoder[A](using elemEncoder: Encoder[A]) as Encoder[List[A]]:
    def encode(value: List[A]): Json = Json.Arr(value.map(elemEncoder.encode))

end EncoderInstances

/**
 * A specialization of `Encoder` that returns JSON objects only
 */
trait ObjectEncoder[-A] extends Encoder[A]:
  // Refines the encoding result to `Json.Obj`
  def encode(value: A): Json.Obj

  /**
   * Combines `this` encoder with `that` encoder.
   * Returns an encoder producing a JSON object containing both
   * fields of `this` encoder and fields of `that` encoder.
   */
  def zip[B](that: ObjectEncoder[B]): ObjectEncoder[(A, B)] =
    val originalEncoder = this

    new ObjectEncoder[(A, B)]:
      def encode(value: (A, B)): Json.Obj =
        Json.Obj(originalEncoder.encode(value._1).fields ++ that.encode(value._2).fields)
  end zip

end ObjectEncoder

object ObjectEncoder:

  /**
   * An encoder for values of type `A` that produces a JSON object with one field
   * named according to the supplied `name` and containing the encoded value.
   */
  def field[A](name: String)(using encoder: Encoder[A]): ObjectEncoder[A] =
    new ObjectEncoder[A]:
      def encode(value: A): Json.Obj =
        Json.Obj(Map(name -> encoder.encode(value)))

end ObjectEncoder

/**
 * The dual of an encoder. Decodes a serialized value into its initial type `A`.
 */
trait Decoder[+A]:

  /**
   * @param data The data to de-serialize
   * @return The decoded value wrapped in `Some`, or `None` if decoding failed
   */
  def decode(data: Json): Option[A]

  /**
   * Combines `this` decoder with `that` decoder.
   * Returns a decoder that invokes both `this` decoder and `that`
   * decoder and returns a pair of decoded value in case both succeed,
   * or `None` if at least one failed.
   */
  def zip[B](that: Decoder[B]): Decoder[(A, B)] =
    val originalDecoder = this

    new Decoder[(A, B)]:
      def decode(data: Json): Option[(A, B)] =
        originalDecoder.decode(data).zip(that.decode(data))
  end zip

  /**
   * Transforms this `Decoder[A]` into a `Decoder[B]`, given a transformation function
   * from `A` to `B`.
   *
   * This operation is also known as “map”.
   */
  def transform[B](f: A => B): Decoder[B] =
    val originalDecoder = this

    new Decoder[B]:
      def decode(data: Json): Option[B] =
        originalDecoder.decode(data).map(f)
  end transform

end Decoder

object Decoder extends DecoderInstances

trait DecoderInstances:

  /** A decoder for the `Unit` value */
  given Decoder[Unit]:
    def decode(data: Json): Option[Unit] = data match
      case Json.Null =>
        Some(())
      case _ =>
        None

  /** A decoder for `Int` values. Hint: use the `isValidInt` method of `BigDecimal`. */
  // TODO Define an anonymous given instance of type `Decoder[Int]`
  given Decoder[Int]:
    def decode(data: Json): Option[Int] = data match
      case Json.Num(x) => if x.isValidInt then Some(x.intValue()) else None
      case _ => None

  /** A decoder for `String` values */
  // TODO Define an anonymous given instance of type `Decoder[String]`
  given Decoder[String]:
    def decode(data: Json): Option[String] = data match
      case Json.Str(x) => Some(x)
      case _ => None

  /** A decoder for `Boolean` values */
  // TODO Define an anonymous given instance of type `Decoder[Boolean]`
  given Decoder[Boolean]:
    def decode(data: Json): Option[Boolean] = data match
      case Json.Bool(x) => Some(x)
      case _ => None
  /**
   * A decoder for JSON arrays. It decodes each item of the array
   * using the given `elemDecoder`. The resulting decoder succeeds only
   * if all the JSON array items are successfully decoded.
   */
  given listDecoder[A](using elemDecoder: Decoder[A]) as Decoder[List[A]]:
    def decode(data: Json): Option[List[A]] = data match
      case Json.Arr(x) => Some(x.flatMap(elemDecoder.decode))
      case _ => None


  /**
   * A decoder for JSON objects. It decodes the value of a field of
   * the supplied `name` using the given `decoder`.
   */
  def field[A](name: String)(implicit decoder: Decoder[A]): Decoder[A] =
    new Decoder[A]:
      def decode(data: Json): Option[A] = data match
        case Json.Obj(x) => decoder.decode(x(name))
        case _ => None


end DecoderInstances

case class Person(name: String, age: Int)

object Person extends PersonCodecs

trait PersonCodecs:

  /** The encoder for `Person` */
  given Encoder[Person] =
    ObjectEncoder.field[String]("name")
      .zip(ObjectEncoder.field[Int]("age"))
      .transform[Person](user => (user.name, user.age))

  /** The corresponding decoder for `Person` */
  given Decoder[Person] =
    Decoder.field[String]("name").zip(Decoder.field[Int]("age")).transform[Person]((name, age) => Person(name, age))

end PersonCodecs

case class Contacts(people: List[Person])

object Contacts extends ContactsCodecs

trait ContactsCodecs:

  // TODO Define the encoder and the decoder for `Contacts`
  // The JSON representation of a value of type `Contacts` should be
  // a JSON object with a single field named “people” containing an
  // array of values of type `Person` (reuse the `Person` codecs)
  given Encoder[Contacts] =
    ObjectEncoder.field[List[Person]]("people").transform[Contacts](c => c.people)

  given Decoder[Contacts] =
    Decoder.field[List[Person]]("people").transform[Contacts](p => Contacts(p))

end ContactsCodecs

import Util._

// In case you want to try your code, here is a simple main that can be used as
// a starting point, run it using the `run` sbt task. Otherwise, you can use the
// REPL (use the `console` sbt task).
@main def run(): Unit =
  println(renderJson(42))
  println(renderJson("foo"))

  val maybeJsonString = parseJson(""" "foo" """)
  val maybeJsonObj    = parseJson(""" { "name": "Alice", "age": 42 } """)
  val maybeJsonObj2   = parseJson(""" { "name": "Alice", "age": "42" } """)
  // Uncomment the following lines as you progress in the assignment
  // println(maybeJsonString.flatMap(_.decodeAs[Int]))
  // println(maybeJsonString.flatMap(_.decodeAs[String]))
  // println(maybeJsonObj.flatMap(_.decodeAs[Person]))
  // println(maybeJsonObj2.flatMap(_.decodeAs[Person]))
  // println(renderJson(Person("Bob", 66)))
