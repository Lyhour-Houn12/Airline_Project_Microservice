-- An extra's code is unique within its airline.
-- The entity declared this constraint on columns that don't exist, so on a new database Postgres
-- refused to create the ancillaries table at all (Hibernate only logged a warning). Fixed in the entity;
-- databases made before the constraint existed get it here.
ALTER TABLE ancillaries ADD CONSTRAINT uk_ancillary_airline_code UNIQUE (airline_id, code);
