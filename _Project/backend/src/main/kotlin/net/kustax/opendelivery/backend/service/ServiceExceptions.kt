package net.kustax.opendelivery.backend.service

class NotFoundException(message: String) : Exception(message)
class ConflictException(message: String) : Exception(message)
class ForbiddenException(message: String) : Exception(message)
