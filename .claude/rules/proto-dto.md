# Proto DTO Rules

Conventions for protobuf DTO message files in `src/main/proto/`.

## File Naming

- DTO files must be named `<name>.dto.proto` (e.g., `game.dto.proto`, `aggregator.dto.proto`)
- Place DTO files in the `dto/` subdirectory (e.g., `src/main/proto/game/v1/dto/`)

## Message Naming

- Message names must follow the pattern `<Name>Dto` (e.g., `GameDto`, `AggregatorDto`, `ProviderDto`)
- Enum names in DTO files must follow the pattern `<Name>Dto` (e.g., `PlatformDto`)

## Field Formatting

- Each field on its own line
- Blank line between every field

```protobuf
// CORRECT
message GameDto {
  string identity = 1;

  string name = 2;

  bool active = 3;
}

// WRONG - no blank lines between fields
message GameDto {
  string identity = 1;
  string name = 2;
  bool active = 3;
}
```

## Standard Header

Every DTO proto file must include:

```protobuf
syntax = "proto3";

package game.v1;

option java_multiple_files = true;
option java_package = "com.nekgamebling.game.v1";
```
