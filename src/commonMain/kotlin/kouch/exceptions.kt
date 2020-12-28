package kouch


class KouchDatabaseException(message: String) : Exception(message)
class KouchDesignViewResponseException(message: String) : Exception(message)
class KouchDesignStandardResponseException(message: String) : Exception(message)
class KouchDocumentException(message: String) : Exception(message)
class KouchUserException(message: String) : Exception(message)
class KouchServerException(message: String): Exception(message)

class UnsupportedStatusCodeException(message: String) : Exception(message)

class IdIsBlankException(message: String) : Exception(message)

class RevisionIsNullException(message: String) : Exception(message)
class RevisionIsNotNullException(message: String) : Exception(message)
class ResponseRevisionIsNullException(message: String) : Exception(message)

class DocIsNullException(message: String) : Exception(message)

class BlankUserNameException(message: String) : Exception(message)

class BulkUpsertFailed(message: String) : Exception(message)
class PutFailed(message: String) : Exception(message)
