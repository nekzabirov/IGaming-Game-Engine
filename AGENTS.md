# AGENTS.md

Guidance for coding agents working in this repository lives in [CLAUDE.md](CLAUDE.md): what the
service is, the flat Ktor layout, the rules that must hold (entities never leave a transaction, no
remote call inside one, frozen event wire shape, exception names on the wire), and how to build,
run and test.
