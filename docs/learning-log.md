# Session 1

- JDBC vs JPA vs Hibernate
- Flyway
- validate vs update

# Session 2

- Master Data
- Transaction Data
- ENUM
- CHECK Constraints

# Session 3

- One-to-One Relationship
- Shared Primary Key
- Foreign Key

# actually learned so far

- # Java
- Packages
- Classes
- Interfaces
- Enums
- Long vs long
- String for identifiers such as phone numbers
- BigDecimal
- OffsetDateTime
- Primitive vs wrapper types
- Naming conventions

- # JPA/Hibernate
- @Entity

- @Table
- @Id
- @GeneratedValue
- @Column
- @Enumerated
- EnumType.STRING

- and importantly:

- Hibernate maps Java objects to the database; it doesn't mean Hibernate should own our schema.

- # PostgreSQL
- BIGSERIAL
- BIGINT
- VARCHAR
- TEXT
- NUMERIC(precision, scale)
- BOOLEAN
- TIMESTAMPTZ
- PostgreSQL ENUM
- Primary keys
- NOT NULL
- DEFAULT
- constraints

- # Flyway
- the critical rule:

Never casually modify an already-applied migration.

We actually encountered a checksum mismatch earlier, so this isn't just theoretical. Flyway validates migration checksums and rejects a changed applied migration.

- # Git
- git init
- .gitignore
- staging
- git add
- git status
- commits
- why application.yaml must stay out of Git
- why environment credentials shouldn't be committed
- meaningful checkpoints