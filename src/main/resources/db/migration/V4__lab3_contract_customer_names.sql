-- Deploy the application that reads/writes split names to every instance first.
SET LOCAL lock_timeout = '5s';
DROP TRIGGER customer_names_sync ON customers;
DROP FUNCTION sync_customer_names();
ALTER TABLE customers DROP COLUMN full_name;
