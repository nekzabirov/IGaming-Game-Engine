package domain.exception.system

class AggregatorUnavailableException(integration: String) :
    SystemException("Aggregator is unavailable: $integration")
