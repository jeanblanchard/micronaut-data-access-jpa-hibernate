drop table if exists genre;
drop table if exists book;

create table genre (
                       id    bigint serial primary key not null,
                       name varchar(255)              not null unique
);

create table book (
                      id    bigint serial primary key not null,
                      name varchar(255)              not null,
                      isbn varchar(255)              not null,
                      genre_id bigint,
                      constraint fkm1t3yvw5i7olwdf32cwuul7ta
                          foreign key (genre_id) references genre
);

create sequence hibernate_sequence start with 1 increment by 1
