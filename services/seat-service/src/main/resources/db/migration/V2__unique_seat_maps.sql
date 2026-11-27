-- One seat map per cabin class.
-- The entity declared this constraint on columns that don't exist, so on a new database Postgres
-- refused to create the seat_maps table at all (Hibernate only logged a warning). Fixed in the entity;
-- databases made before the constraint existed get it here.
ALTER TABLE seat_maps ADD CONSTRAINT uk_seat_map_cabin_class UNIQUE (cabin_class_id);
