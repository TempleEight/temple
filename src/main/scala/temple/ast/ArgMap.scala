package temple.ast

import temple.DSL.syntax.Arg
import temple.DSL.semantics.SemanticContext

/**
  * A wrapper around a map of arguments, as produced by [[temple.DSL.semantics#parseParameters]], with methods added
  * to extract arguments of given types
  *
  * @param argMap The underlying immutable map of names to argument values
  */
case class ArgMap(argMap: Map[String, Arg]) {

  /**
    * Type-safely extract an argument from the argument map
    * @param key The name of the argument to extract
    * @param argType The type of the argument to extract
    * @param context The location of the function call, used in the error message
    * @tparam T The type of the element to extract
    * @return The typesafe extracted value
    */
  def getArg[T](key: String, argType: ArgType[T])(implicit context: SemanticContext): T =
    argType.extractArg(argMap(key)).getOrElse {
      throw context.error(s"${argType.stringRep.capitalize} expected at $key, found ${argMap(key)},")
    }

  /**
    * Type-safely extract [[Some]] argument from the argument map, or [[None]] if the default value is
    * [[temple.DSL.syntax.Arg.NoArg]]
    * @param key The name of the argument to extract
    * @param argType The type of the argument to extract
    * @param context The location of the function call, used in the error message
    * @tparam T The type of the element to extract
    * @return [[Some]] typesafe extracted value, or [[None]] if it was not provided and the default was
    *         [[temple.DSL.syntax.Arg.NoArg]]
    */
  def getOptionArg[T](key: String, argType: ArgType[T])(implicit context: SemanticContext): Option[T] =
    argMap(key) match { case Arg.NoArg => None; case _ => Some(getArg(key, argType)) }
}
