package tools

enum Integrator:
  case ExplicitEuler
  case SemiImplicitEuler
  case Verlet
  case RK2

  override def toString: String = this match
    case ExplicitEuler => "Explicit Euler"
    case SemiImplicitEuler => "Semi-impl. Euler"
    case Verlet => "Verlet"
    case RK2 => "RK2"