package domain.exception.badrequest

class AggregatorNotSupportedException(integration: String) :
    BadRequestException("Unsupported aggregator integration: $integration")
