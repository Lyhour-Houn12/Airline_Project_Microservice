-- A flight instance no longer needs a schedule: one-off flights (extra holiday flights, charters,
-- replacements) are created directly with POST /api/v1/flight-instances. Instances generated
-- from a schedule still record it.
ALTER TABLE flight_instances ALTER COLUMN schedule_id DROP NOT NULL;
