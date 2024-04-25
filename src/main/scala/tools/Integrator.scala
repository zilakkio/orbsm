package tools

/** Numerical integration algorithms
     */
enum Integrator:
  case ExplicitEuler
  case SemiImplicitEuler
  case Verlet
  case RK2
  case RK4

  override def toString: String = this match
    case ExplicitEuler => "Explicit Euler"
    case SemiImplicitEuler => "SI Euler"
    case Verlet => "Verlet"
    case RK2 => "RK2"
    case RK4 => "RK4"