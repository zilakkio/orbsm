package tools

import engine.{Body, Vector3D}

/** Camera position modes
     */
enum Centering:
  case MassCenter
  case Custom(pos: Vector3D)
  case AtBody(body: Body)