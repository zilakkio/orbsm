package tools

enum MultiSelectMode:
  case Closest2D
  case Closest3D
  case Heaviest
  case Largest

  override def toString: String = this match
    case Closest2D => "Closest (2D)"
    case Closest3D => "Closest (3D)"
    case Heaviest => "Heaviest"
    case Largest => "Largest"
