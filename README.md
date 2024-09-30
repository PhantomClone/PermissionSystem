# PermissionSystem
Bewerbungsaufgabe für PlayLegend - PermissionSystem


Used libraries:
- Sadu: Simple abstraction for database access. [sadu github](https://github.com/rainbowdashlabs/sadu)

Way to improve:
implement cache for Rank





Database structor:

Tables:

Table permission {
id INT [primary key, increment]
name VARCHAR(50) [unique, not null]
}

Table rank {
id INT [primary key, increment]
priority INT [unique, not null]
name VARCHAR(50) [not null]
prefix VARCHAR(20) [not null]
base_rank_name INT [ref: - rank.id, null]
}

Table rank_permission {
id INT [primary key, increment]
permission_id INT [ref: - permission.id, not null]
rank_id INT [ref: > rank.id, not null]
}

Table user_permission {
permission_id INT [primary key, ref: - permission.id]
uuid BINARY(16) [primary key]
since timestamp [not null]
until timestamp [null]
}

Table user_rank {
uuid BINARY(16) [primary key]
rank_id INT [primary key, ref: < rank.id]
since timestamp [not null]
until timestamp [null]
}

![img.png](permission_tables.png)