package korolev.state

import korolev.state.EnvConfigurator.Env
import korolev.{Async, Context}

trait EnvConfigurator[F[+_], S, M] {

  def configure(access: Context.BaseAccess[F, S, M])
               (implicit F: Async[F]): F[Env[F, M]]

}

object EnvConfigurator {

  case class Env[F[+_], M](onDestroy: () => F[Unit],
                           onMessage: PartialFunction[M, F[Unit]] = PartialFunction.empty)

  def default[F[+_], S, M]: EnvConfigurator[F, S, M] =
    new DefaultEnvConfigurator

  def apply[F[+_], S, M](f: Context.BaseAccess[F, S, M] => F[Env[F, M]]): EnvConfigurator[F, S, M] =
    new EnvConfigurator[F, S, M] {
      override def configure(access: Context.BaseAccess[F, S, M])
                            (implicit F: Async[F]): F[Env[F, M]] =
        f(access)
    }

  private class DefaultEnvConfigurator[F[+_], S, M] extends EnvConfigurator[F, S, M] {
    override def configure(access: Context.BaseAccess[F, S, M])
                          (implicit F: Async[F]): F[Env[F, M]] =
      Async[F].pure(Env(onDestroy = () => Async[F].unit, PartialFunction.empty))
  }

}
