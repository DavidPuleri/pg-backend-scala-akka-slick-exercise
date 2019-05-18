create schema pg collate utf8mb4_0900_ai_ci;

create table items
(
	id char(36) not null
		primary key,
	name varchar(255) null,
	price double(11,2) null
);

create table users
(
	id char(36) not null,
	firstName varchar(255) not null,
	lastName varchar(255) not null,
	email varchar(255) not null,
	constraint users_email_uindex
		unique (email),
	constraint users_id_uindex
		unique (id)
);

alter table users
	add primary key (id);

create table orders
(
	id char(36) not null
		primary key,
	userId char(36) null,
	status char(36) null,
	houseNumber int not null,
	streetName varchar(255) null,
	postCode varchar(255) null,
	city varchar(255) null,
	constraint orders_users_user_id_fk
		foreign key (userId) references users (id)
			on delete cascade
);

create table carts
(
	orderId char(36) not null,
	itemId char(36) not null,
	quantity int not null,
	price double(11,2) null,
	primary key (orderId, itemId),
	constraint carts_items_id_fk
		foreign key (itemId) references items (id)
			on delete cascade,
	constraint carts_orders_id_fk
		foreign key (orderId) references orders (id)
			on delete cascade
);

