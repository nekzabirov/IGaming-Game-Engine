CREATE TABLE aggregators (
    id BIGSERIAL PRIMARY KEY,
    identity VARCHAR(255) NOT NULL,
    integration VARCHAR(255) NOT NULL,
    config JSON NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE UNIQUE INDEX aggregators_identity_unique ON aggregators (identity);

CREATE TABLE providers (
    id BIGSERIAL PRIMARY KEY,
    identity VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    images JSON NOT NULL,
    sort_order INT NOT NULL DEFAULT 100,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    aggregator_id BIGINT NOT NULL REFERENCES aggregators (id),
    blocked_country JSON NOT NULL DEFAULT '[]'
);
CREATE UNIQUE INDEX providers_identity_unique ON providers (identity);

CREATE TABLE collections (
    id BIGSERIAL PRIMARY KEY,
    identity VARCHAR(255) NOT NULL,
    name JSON NOT NULL,
    images JSON NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 100
);
CREATE UNIQUE INDEX collections_identity_unique ON collections (identity);

CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    identity VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    provider_id BIGINT NOT NULL REFERENCES providers (id),
    bonus_bet_enable BOOLEAN NOT NULL DEFAULT TRUE,
    bonus_wagering_enable BOOLEAN NOT NULL DEFAULT TRUE,
    tags JSON NOT NULL,
    active BOOLEAN NOT NULL,
    images JSON NOT NULL,
    sort_order INT NOT NULL
);
CREATE UNIQUE INDEX games_identity_unique ON games (identity);

CREATE TABLE game_variants (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    integration VARCHAR(255) NOT NULL,
    game_id BIGINT NOT NULL REFERENCES games (id),
    provider_name VARCHAR(255) NOT NULL,
    free_spin_enable BOOLEAN NOT NULL,
    free_chip_enable BOOLEAN NOT NULL,
    jackpot_enable BOOLEAN NOT NULL,
    demo_enable BOOLEAN NOT NULL,
    bonus_buy_enable BOOLEAN NOT NULL,
    locales JSON NOT NULL,
    platforms JSON NOT NULL,
    play_lines INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX game_variants_symbol_unique ON game_variants (symbol);
CREATE INDEX game_variants_game_id_idx ON game_variants (game_id);

CREATE TABLE game_collections (
    game_id BIGINT NOT NULL REFERENCES games (id),
    collection_id BIGINT NOT NULL REFERENCES collections (id),
    sort_order INT NOT NULL DEFAULT 100,
    PRIMARY KEY (game_id, collection_id)
);
CREATE INDEX game_collections_collection_id_idx ON game_collections (collection_id);

CREATE TABLE game_favourites (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games (id),
    player_id VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX game_favourites_player_game_unique ON game_favourites (player_id, game_id);

CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    game_variant_id BIGINT NOT NULL REFERENCES game_variants (id),
    player_id VARCHAR(255) NOT NULL,
    token VARCHAR(1024) NOT NULL,
    external_token VARCHAR(1024),
    currency VARCHAR(10) NOT NULL,
    locale VARCHAR(10) NOT NULL,
    platform VARCHAR(20) NOT NULL
);
CREATE INDEX sessions_player_id_idx ON sessions (player_id);
CREATE UNIQUE INDEX sessions_token_unique ON sessions (token);

CREATE TABLE rounds (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    freespin_id VARCHAR(255),
    session_id BIGINT NOT NULL REFERENCES sessions (id),
    game_variant_id BIGINT NOT NULL REFERENCES game_variants (id),
    created_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP
);
CREATE UNIQUE INDEX rounds_external_id_session_id_unique ON rounds (external_id, session_id);

CREATE TABLE spins (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) NOT NULL,
    round_id BIGINT NOT NULL REFERENCES rounds (id),
    reference_id BIGINT REFERENCES spins (id),
    type VARCHAR(20) NOT NULL,
    amount BIGINT NOT NULL,
    real_amount BIGINT NOT NULL,
    bonus_amount BIGINT NOT NULL
);
CREATE INDEX spins_external_id_idx ON spins (external_id);
CREATE INDEX spins_round_id_idx ON spins (round_id);
