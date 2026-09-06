<div align="center">

# Casino Engine

**Open-source game aggregator core for iGaming platforms.**

The game-mechanics engine powering casinos on [1638.cloud](https://1638.cloud) —
released under Apache 2.0 so any operator can audit, fork, or drop it into
their existing stack.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-D4AF37.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg)](https://kotlinlang.org)
[![JVM](https://img.shields.io/badge/JVM-21-0A2818.svg)](https://openjdk.org)
[![Built in Malta](https://img.shields.io/badge/Built%20in-Malta-D4AF37.svg)](https://1638.cloud)

[Website](https://1638.cloud) · [Live on moonbet.casino](https://moonbet.casino) · [Live on 2k.ua](https://2k.ua) · [Report an issue](https://github.com/nekzabirov/IGaming-Game-Engine/issues/new/choose)

</div>

---

## What this is

Casino Engine is a production-grade Kotlin microservice that handles the
**game side of an iGaming platform**: provider/aggregator integrations,
session orchestration, betting logic, freespin mechanics, and round
lifecycle. It runs in production on [moonbet.casino](https://moonbet.casino)
and [2k.ua](https://2k.ua).

It's the **only open-source module** of the [1638.cloud](https://1638.cloud)
platform — the other seven engines (PAM, Wallet, Payment, Risk, Engagement,
Intelligence, CMS) are proprietary. Operators can use Casino Engine standalone
through custom adapters, or run it as part of the full 1638.cloud white-label
platform.

> **Why open-source?** Operators deserve to see the code that handles their
> money flow. Aggregator integrations should be auditable. Game session logic
> should not be a black box. Every line is on GitHub.

---

## Who this is for

| You are... | What this gives you |
| --- | --- |
| **A casino operator** running your own platform | A production-grade aggregator integration layer you can drop into your stack. No vendor lock-in, no rev share, no licence fees. |
| **A platform engineer** evaluating iGaming infrastructure | A reference implementation of launch, bet, round, and freespin mechanics in Kotlin/Ktor. |
| **A startup** thinking about launching a casino | A way to validate the technical model before committing. When you're ready to go live, [1638.cloud](https://1638.cloud) handles the licence, payments, KYC, and the rest. |

---

## Features

- **Aggregator integrations** — Pragmatic Play, OneGameHub, Pateplay, with a
  documented pattern for adding more
- **Full betting lifecycle** — PLACE, SETTLE, ROLLBACK with idempotency
  guarantees
- **Freespin mechanics** — preset retrieval, creation, cancellation
- **Round management** — first-bet creation, multi-bet round reuse via
  external ID, finish lifecycle
- **Event-driven** — RabbitMQ publisher for session, spin, round, and game
  events
- **Flat Ktor application** — `plugins/`, `db/` (Exposed entities are the model),
  `services/`, `grpc/`; no layers to cross, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **gRPC API** — `game.v1` protocol with 5 services and published client JAR
- **Pluggable adapters** — Wallet, PlayerLimit, File, Currency, Event ports
  you implement for your stack
- **Production-tested** — runs on moonbet.casino and 2k.ua

---

## Quick start

```bash
# 1. Clone
git clone https://github.com/nekzabirov/IGaming-Game-Engine.git
cd IGaming-Game-Engine

# 2. Start infrastructure
docker compose up -d postgres rabbitmq redis

# 3. Configure
cp .env.example .env

# 4. Run
./gradlew run                  # HTTP :8080, gRPC :5050
```

Full setup, integration guides, and operations docs are in [`docs/`](./docs) — see the [Documentation](#documentation) section below.

---

## Tech stack

| Component | Choice |
| --- | --- |
| Language | Kotlin 2.0.21 on JVM 21 |
| HTTP server | Ktor 3.0 (CIO) |
| RPC | gRPC + Protobuf |
| Database | PostgreSQL via Exposed ORM |
| Messaging | RabbitMQ |
| Cache | Redis (Lettuce) |
| Object storage | S3-compatible (MinIO local) |
| DI | Koin |
| Build | Gradle (Kotlin DSL) |

Architecture: hexagonal (ports & adapters) with CQRS.

---

## Documentation

Technical documentation lives under [`docs/`](./docs):

- [Architecture](./docs/ARCHITECTURE.md) — system design, layers, source structure, event flow
- [Integrations](./docs/INTEGRATIONS.md) — supported aggregators and how to add a new one
- [Adapters](./docs/ADAPTERS.md) — required adapters (Wallet, PlayerLimit, File, Event, Currency) you implement for production
- [API](./docs/API.md) — gRPC API reference (`game.v1` package)
- [Configuration](./docs/CONFIGURATION.md) — environment variables and infrastructure
- [Errors](./docs/ERRORS.md) — domain exception hierarchy and gRPC status mapping

---

## The bigger picture — 1638.cloud platform

Casino Engine is one of eight engines that make up the
[1638.cloud](https://1638.cloud) iGaming platform:

| # | Engine | Status |
| --- | --- | --- |
| 01 | **Casino Engine** — game mechanics, provider integrations | **Open-source** (this repo) |
| 02 | PAM Engine — player accounts, KYC, lifecycle | Proprietary |
| 03 | Wallet Engine — multi-currency, real-time balance | Proprietary |
| 04 | Payment Engine — 76+ providers, fiat & crypto | Proprietary |
| 05 | Risk Engine — anti-fraud, AML, behavioural scoring | Proprietary |
| 06 | Engagement Engine — bonuses, tournaments, loyalty | Proprietary |
| 07 | Intelligence Engine — segmentation, churn, LTV | Proprietary |
| 08 | CMS Engine — content, theming, localisation | Proprietary |

Operators can use the full stack as a white-label or turnkey deployment
under our [Anjouan](https://1638.cloud) master licence, or drop individual
engines into their existing platform. Casino Engine works in both modes.

**Go-live from contract:** 7 days. **Setup:** from €0 with Founders Circle.
See [1638.cloud](https://1638.cloud) for full terms.

---

## Want to use this in production?

You have three paths:

**1. Run it yourself (free, Apache 2.0).**
Implement the adapters, host the infrastructure, integrate with your own
wallet and player systems. The code is yours. We don't charge, we don't
audit, we don't gatekeep.

**2. Use Casino Engine on the 1638.cloud platform.**
Drop into our managed stack — we run the infrastructure, you keep the
flexibility. Useful when you want the open-source engine but not the ops
burden.

**3. Full white-label on 1638.cloud.**
Casino Engine plus the other seven engines, plus our Anjouan master
licence. Live in 7 days. From €0 setup. Contact
[customer@1638.cloud](mailto:customer@1638.cloud).

---

## Contributing

Issues, discussions, and pull requests are welcome. Before contributing,
please read [`CONTRIBUTING.md`](./CONTRIBUTING.md).

We're especially interested in:
- New aggregator integration adapters
- Bug reports from production deployments
- Performance benchmarks and profiling reports
- Documentation improvements

---

## Security

This is financial infrastructure. If you find a security issue, **do not
open a public issue** — see [`SECURITY.md`](./SECURITY.md) for responsible
disclosure.

---

## Licence

Apache 2.0 — see [`LICENSE`](./LICENSE).

You can use Casino Engine commercially, modify it, distribute it, and run
it on any operator platform without paying royalties to 1638.cloud. The
only requirements are attribution and not using the 1638.cloud trademarks
without permission.

---

## About 1638.cloud

[1638.cloud](https://1638.cloud) is a B2B iGaming platform built in Malta.
We provide white-label and turnkey casino infrastructure under our Anjouan
master licence. We open-source the Casino Engine because game mechanics
should not be a black box — and because the best way to earn an operator's
trust is to let them read the code first.

**Founder:** [Nekbakht Zabirov](https://github.com/nekzabirov)
**Email:** [customer@1638.cloud](mailto:customer@1638.cloud)
**Web:** [1638.cloud](https://1638.cloud)

<div align="center">

— Built for operators who move first —

</div>
