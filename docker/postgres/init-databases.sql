-- Runs once, when the postgres volume is first created. One database per service;
-- each service's Flyway migrations create its tables.
CREATE DATABASE airline_core_db;
CREATE DATABASE location_service;
CREATE DATABASE flight_ops_db;
CREATE DATABASE pricing_db;
CREATE DATABASE seat_db;
CREATE DATABASE ancillary_db;
CREATE DATABASE booking_db;
CREATE DATABASE payment_db;
CREATE DATABASE user_db;
