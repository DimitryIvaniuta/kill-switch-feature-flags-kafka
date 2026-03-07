create table if not exists feature_flag_state (
  flag_name varchar(200) primary key,
  enabled boolean not null,
  version bigint not null,
  updated_at timestamptz not null,
  updated_by varchar(200) not null,
  reason varchar(1000) not null
);

create table if not exists feature_flag_audit (
  id uuid primary key,
  flag_name varchar(200) not null,
  enabled boolean not null,
  version bigint not null,
  action varchar(32) not null,
  changed_at timestamptz not null,
  changed_by varchar(200) not null,
  reason varchar(1000) not null
);

create index if not exists idx_feature_flag_audit_flag_version
  on feature_flag_audit(flag_name, version desc);

create table if not exists outbox_events (
  id uuid primary key,
  aggregate_type varchar(64) not null,
  aggregate_id varchar(200) not null,
  event_type varchar(200) not null,
  payload_json text not null,
  created_at timestamptz not null,
  published_at timestamptz null,
  attempts int not null,
  last_error varchar(2000) null
);

create index if not exists idx_outbox_pending
  on outbox_events(published_at, created_at);
