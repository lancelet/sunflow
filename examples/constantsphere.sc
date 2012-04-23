image {
  resolution 400 400
  aa 0 1
  filter mitchell
}

camera {
  type pinhole
  eye    0 0 5
  target 0 0 0
  up     0 1 0
  fov    60
  aspect 1.0
}

shader {
  name Constant
  type constant
  color 1 1 1
}

object {
  shader Constant
  type sphere
  c 0 0 0
  r 2
}
