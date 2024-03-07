import engine.{Body, Vector3D}

enum Centering:
  case MassCenter
  case Custom(pos: Vector3D)
  case AtBody(body: Body)