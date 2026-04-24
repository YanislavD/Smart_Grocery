create table users (
                       id bigserial primary key,
                       email varchar(255) not null unique,
                       password_hash varchar(255) not null,
                       created_at timestamp not null default now()
);

create table households (
                            id bigserial primary key,
                            name varchar(120) not null,
                            owner_id bigint not null references users(id)
);

create table household_members (
                                   id bigserial primary key,
                                   household_id bigint not null references households(id),
                                   user_id bigint not null references users(id),
                                   role varchar(30) not null,
                                   unique (household_id, user_id)
);

create table products (
                          id bigserial primary key,
                          name varchar(120) not null,
                          category varchar(60),
                          default_unit varchar(30)
);

create table pantry_items (
                              id bigserial primary key,
                              household_id bigint not null references households(id),
                              product_id bigint not null references products(id),
                              qty numeric(10,2) not null,
                              unit varchar(30) not null,
                              expiry_date date,
                              min_qty_threshold numeric(10,2) not null default 1
);

create table shopping_lists (
                                id bigserial primary key,
                                household_id bigint not null references households(id),
                                status varchar(30) not null,
                                created_at timestamp not null default now()
);

create table shopping_list_items (
                                     id bigserial primary key,
                                     shopping_list_id bigint not null references shopping_lists(id),
                                     product_id bigint not null references products(id),
                                     qty numeric(10,2) not null,
                                     checked boolean not null default false
);