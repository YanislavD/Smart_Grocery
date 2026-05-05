create table household_invites (
                                   id bigserial primary key,
                                   household_id bigint not null references households(id),
                                   email varchar(255) not null,
                                   token varchar(80) not null unique,
                                   status varchar(30) not null,
                                   invited_by_user_id bigint not null references users(id),
                                   accepted_by_user_id bigint references users(id),
                                   created_at timestamp not null default now(),
                                   accepted_at timestamp
);
