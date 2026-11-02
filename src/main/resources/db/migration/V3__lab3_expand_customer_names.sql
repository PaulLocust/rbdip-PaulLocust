SET LOCAL lock_timeout = '5s';
ALTER TABLE customers ADD COLUMN first_name VARCHAR(255), ADD COLUMN last_name VARCHAR(255);

-- Preserve the exact legacy spelling: split at the first space only.
-- An empty last_name represents a mononym; preserve trailing spaces in first_name.
CREATE FUNCTION sync_customer_names() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.first_name IS NULL THEN
            NEW.first_name := CASE WHEN strpos(NEW.full_name, ' ') IN (0, length(NEW.full_name))
                THEN NEW.full_name ELSE split_part(NEW.full_name, ' ', 1) END;
            NEW.last_name := CASE WHEN strpos(NEW.full_name, ' ') IN (0, length(NEW.full_name)) THEN ''
                ELSE substr(NEW.full_name, strpos(NEW.full_name, ' ') + 1) END;
        ELSE
            NEW.full_name := NEW.first_name || CASE WHEN NEW.last_name = '' THEN ''
                ELSE ' ' || NEW.last_name END;
        END IF;
    ELSIF NEW.full_name IS DISTINCT FROM OLD.full_name THEN
        NEW.first_name := CASE WHEN strpos(NEW.full_name, ' ') IN (0, length(NEW.full_name))
                THEN NEW.full_name ELSE split_part(NEW.full_name, ' ', 1) END;
        NEW.last_name := CASE WHEN strpos(NEW.full_name, ' ') IN (0, length(NEW.full_name)) THEN ''
            ELSE substr(NEW.full_name, strpos(NEW.full_name, ' ') + 1) END;
    ELSE
        NEW.full_name := NEW.first_name || CASE WHEN NEW.last_name = '' THEN ''
            ELSE ' ' || NEW.last_name END;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER customer_names_sync BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION sync_customer_names();

UPDATE customers SET first_name = CASE WHEN strpos(full_name, ' ') IN (0, length(full_name))
        THEN full_name ELSE split_part(full_name, ' ', 1) END,
    last_name = CASE WHEN strpos(full_name, ' ') IN (0, length(full_name)) THEN ''
        ELSE substr(full_name, strpos(full_name, ' ') + 1) END;
ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE customers ALTER COLUMN last_name SET NOT NULL;
