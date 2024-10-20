package com.example.domain

import io.circe.Encoder
import io.circe.generic.extras.semiauto.deriveUnwrappedEncoder

import java.util.UUID

final case class PrincipalId(value: UUID) extends AnyVal

object PrincipalId {

  implicit val principalIdEncoder: Encoder[PrincipalId] = deriveUnwrappedEncoder

}

final case class Principal(id: PrincipalId)
