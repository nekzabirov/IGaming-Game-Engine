-- `tags` belongs to the catalog sync: SyncCasinoCatalogUsecase overwrites it wholesale from the hub
-- on every run, so an editorial tag written there survives only until the next pass. `custom_tags`
-- is the local half — same split that already exists for images (`images` / `custom_images`), and
-- the wire keeps showing one merged list.
ALTER TABLE casino_games     ADD COLUMN custom_tags JSON NOT NULL DEFAULT '[]';
ALTER TABLE casino_providers ADD COLUMN custom_tags JSON NOT NULL DEFAULT '[]';

-- Rescue the one tag that was curated straight into `tags` before this column existed: it drives
-- the "Вибір PreMatch" rail on the home screen, and the next sync would have wiped it without a
-- trace. Everything else in `tags` is the hub's and stays there.
UPDATE casino_games
   SET custom_tags = '["prematch_chose"]'::json
 WHERE tags::text LIKE '%"prematch_chose"%';
