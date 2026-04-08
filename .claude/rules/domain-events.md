# Domain Events

Aggregates raise events during state transitions. Usecases drain the events after the write commits and publish them through `IEventPort` directly. There is no `ApplicationEvent` translation layer — `IEventPort.publish(event: DomainEvent)` takes domain events as-is, and `RabbitMqEventPublisher` does the routing-key + payload mapping in one exhaustive `when (event)`.

## Rules

- Events are **pure values** — `data class` in `domain/event/` implementing the sealed `DomainEvent` interface.
- Aggregates that mutate immutably (data classes) return `WithEvents<T>(value, events)` from their mutator methods. The usecase destructures the result, persists `value`, then calls `eventPort.publishAll(events)` after the DB transaction commits.
- Usecases publish **after** the DB write commits (outside the `dbTransaction { }` block) so a failed transaction never produces phantom events.
- Every concrete `DomainEvent` subtype must be exhausted in `RabbitMqEventPublisher.publish` — the sealed hierarchy makes this a compile-time guarantee. Add a new event → compiler forces you to update the publisher's `when`.

## What should NOT become a domain event

- Pure reads (queries don't produce events)
- Cross-aggregate orchestration steps — that's the usecase's job
- "Created in memory" facts — events represent meaningful downstream concerns. `SessionOpened` is raised after the session persists, not when `SessionFactory.create()` returns.

## Adding a new event

1. Create `domain/event/<Name>.kt` extending `DomainEvent`.
2. Raise it from the relevant aggregate mutator (return it inside `WithEvents` for immutable aggregates).
3. The compiler will flag the missing branch in `RabbitMqEventPublisher.publish` — add the routing key + payload mapping there.
4. Create the corresponding mapper file under `infrastructure/rabbitmq/mapper/<Name>Mapper.kt` with its `Payload` data class, `ROUTING_KEY` const, and `toPayload(event)` function.

## Existing events

- `SessionOpened` → `session.opened` — raised after `OpenSessionUsecase` persists the session
- `SpinPlaced` / `SpinSettled` / `SpinRolledBack` → `spin.placed` / `spin.settled` / `spin.rollback` — produced by `Spin.toDomainEvent()` in `ProcessSpinUsecase`
- `RoundFinished` → `round.finished` — returned from `Round.finish()` as `WithEvents<Round>` and drained by `FinishRoundUsecase`
