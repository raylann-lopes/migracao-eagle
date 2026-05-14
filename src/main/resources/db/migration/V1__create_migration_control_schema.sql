create table migration_processes (
    id uuid not null primary key,
    client_name varchar(120) not null,
    cnpj varchar(18),
    eagle_version varchar(30) not null,
    migrator_database varchar(500) not null,
    clean_database_path varchar(500) not null,
    eagle_working_database_path varchar(500),
    final_database_path varchar(500),
    final_database_filename varchar(255),
    status varchar(40) not null,
    default_district_id integer,
    default_cep varchar(10),
    company_state varchar(2),
    migrate_receivables boolean not null,
    last_error varchar(2048),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table migration_sheets (
    id uuid not null primary key,
    process_id uuid not null,
    module varchar(40) not null,
    status varchar(40) not null,
    original_filename varchar(255) not null,
    total_rows integer not null,
    valid_rows integer not null,
    error_count integer not null,
    warning_count integer not null,
    uploaded_at timestamp with time zone not null,
    validated_at timestamp with time zone,
    imported_at timestamp with time zone,
    constraint fk_migration_sheets_process foreign key (process_id) references migration_processes(id)
);

create table migration_rows (
    id uuid not null primary key,
    sheet_id uuid not null,
    row_number integer not null,
    valid boolean not null,
    normalized_json text not null,
    errors_json text,
    warnings_json text,
    constraint fk_migration_rows_sheet foreign key (sheet_id) references migration_sheets(id)
);

create table procedure_executions (
    id uuid not null primary key,
    process_id uuid not null,
    step_order integer not null,
    procedure_name varchar(80) not null,
    status varchar(20) not null,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    error_message varchar(2048),
    constraint fk_procedure_executions_process foreign key (process_id) references migration_processes(id)
);

create index idx_migration_sheets_process_module on migration_sheets(process_id, module);
create index idx_migration_rows_sheet on migration_rows(sheet_id);
create index idx_procedure_executions_process_status on procedure_executions(process_id, status, step_order);
