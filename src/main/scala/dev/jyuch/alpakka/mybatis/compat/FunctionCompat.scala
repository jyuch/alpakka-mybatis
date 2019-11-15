package dev.jyuch.alpakka.mybatis.compat

import java.util.function.{BiFunction, Supplier, Function}

object FunctionCompat {

  def supplierToFunction0[T](func: Supplier[T]): () => T = {
    () => func.get()
  }

  def functionToFunction1[T, R](func: Function[T, R]): T => R = {
    x => func.apply(x)
  }

  def nestedFunctionToFunction1[T1, T2, R](func: Function[T1, Function[T2, R]]): T1 => T2 => R = {
    x => y => func(x)(y)
  }

  def biFunctionToFunction2[T1, T2, R](func: BiFunction[T1, T2, R]): (T1, T2) => R = {
    (x, y) => func.apply(x, y)
  }

}
