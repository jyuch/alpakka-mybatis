package dev.jyuch.alpakka.mybatis.compat

import java.util.function.{BiConsumer, BiFunction, Function, Supplier}

object FunctionCompat {

  def supplierToFunction0[T](func: Supplier[T]): () => T = {
    () => func.get()
  }

  def biConsumerToFunction2[T1, T2](func: BiConsumer[T1, T2]): (T1, T2) => Unit = {
    (x, y) => func.accept(x, y)
  }

  def functionToFunction1[T, R](func: Function[T, R]): T => R = {
    x => func.apply(x)
  }

  def biFunctionToFunction2[T1, T2, R](func: BiFunction[T1, T2, R]): (T1, T2) => R = {
    (x, y) => func.apply(x, y)
  }

}
