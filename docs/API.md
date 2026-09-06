# gRPC API reference

The maintained reference is [`src/main/proto/API.md`](../src/main/proto/API.md) — it is kept in
step with the `.proto` files (a Stop hook enforces it) and documents every service:
`CasinoGameService`, `CasinoProviderService`, `CollectionService`, `CasinoRoundService`,
`WinnerService`, `FreespinService`, `SportbookService`, `JackpotService` (answers UNIMPLEMENTED),
plus the hub-facing `gamehub.v1.WebhookService` implemented by `grpc/WalletGrpcService.kt`.

Error semantics: [ERRORS.md](ERRORS.md).
