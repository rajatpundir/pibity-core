CREATE OR REPLACE FUNCTION inventory.variable_type_function()
RETURNS TRIGGER AS $$
DECLARE
	partition_name TEXT;
    partition_typeName TEXT;
BEGIN
 	partition_name := 'variable_'||NEW.type_name;
    partition_typeName:=NEW.type_name;
IF NOT EXISTS
	(SELECT 1
   	 FROM   information_schema.tables
   	 WHERE  table_name = partition_name)
THEN
	RAISE NOTICE 'A partition has been created %', partition_name;
	EXECUTE format(E'CREATE TABLE %I.%I (CHECK (true)) INHERITS (inventory.variable)', TG_TABLE_SCHEMA, partition_name);
END IF;
EXECUTE format('INSERT INTO %I.%I (organization_id,super_type_name,type_name,super_variable_name,variable_name,auto_generated_id) VALUES($1,$2,$3,$4,$5,$6)', TG_TABLE_SCHEMA, partition_name) using NEW.organization_id, NEW.super_type_name, NEW.type_name, NEW.super_variable_name,NEW.variable_name,NEW.auto_generated_id;
RETURN NULL;
END
$$
LANGUAGE plpgsql;

