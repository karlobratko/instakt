CREATE TABLE users
(
    user_pk            BIGSERIAL PRIMARY KEY,
    username           VARCHAR(50)   NOT NULL,
    email              VARCHAR(256)  NOT NULL,
    first_name         VARCHAR(50)   NOT NULL,
    last_name          VARCHAR(50)   NOT NULL,
    bio                VARCHAR(1024) NOT NULL,
    profile_picture_fk UUID          NULL,
    password_hash      VARCHAR(256)  NOT NULL,
    role               SMALLINT      NOT NULL,
    current_package_fk BIGINT        NOT NULL,
    CONSTRAINT users_username_unique_index UNIQUE (username),
    CONSTRAINT users_email_unique_index UNIQUE (email)
);

-- CREATE TABLE packages
-- (
--     package_pk            SERIAL PRIMARY KEY,
--     name                  VARCHAR(50) NOT NULL,
--     max_uploads_per_month INT         NOT NULL,
--     max_storage_mb        INT         NOT NULL,
--     CONSTRAINT packages_name_unique_index UNIQUE (name)
-- );

CREATE TABLE user_packages
(
    user_package_pk BIGSERIAL PRIMARY KEY,
    user_fk         BIGINT                   NOT NULL,
    package         SMALLINT                 NOT NULL,
--     package_fk      INT                      NOT NULL,
    change_date     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE content_metadata
(
    content_metadata_pk UUID PRIMARY KEY,
    user_fk             BIGINT                   NOT NULL,
    path                VARCHAR(256)             NOT NULL,
    content_type        SMALLINT                 NOT NULL,
    size_bytes          INT                      NOT NULL,
    description         VARCHAR(1024)            NOT NULL,
    uploaded_at         TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE tags
(
    tag_pk              BIGSERIAL PRIMARY KEY,
    name                VARCHAR(50) NOT NULL,
    content_metadata_fk UUID        NOT NULL
);

CREATE TABLE social_media_links
(
    social_media_link_pk BIGSERIAL PRIMARY KEY,
    user_fk              BIGINT       NOT NULL,
    platform             VARCHAR(100) NOT NULL,
    url                  VARCHAR(256) NOT NULL,
    CONSTRAINT social_media_links_user_fk_platform_unique_index UNIQUE (user_fk, platform)
);

CREATE TABLE registration_tokens
(
    registration_token_pk UUID PRIMARY KEY,
    user_fk               BIGINT                   NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at          TIMESTAMP WITH TIME ZONE NULL
);

CREATE TABLE refresh_tokens
(
    refresh_token_pk UUID PRIMARY KEY,
    user_fk          BIGINT                   NOT NULL,
    issued_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at       TIMESTAMP WITH TIME ZONE NULL
);

CREATE TABLE password_reset_tokens
(
    password_reset_token_pk UUID PRIMARY KEY,
    user_fk                 BIGINT                   NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at              TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE users
    ADD FOREIGN KEY (profile_picture_fk) REFERENCES content_metadata (content_metadata_pk);

ALTER TABLE user_packages
    ADD CONSTRAINT fk_user FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;
--     ADD CONSTRAINT fk_package FOREIGN KEY (package_fk) REFERENCES packages (package_pk) ON DELETE CASCADE;

ALTER TABLE content_metadata
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE tags
    ADD FOREIGN KEY (content_metadata_fk) REFERENCES content_metadata (content_metadata_pk) ON DELETE CASCADE;

ALTER TABLE social_media_links
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE registration_tokens
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE password_reset_tokens
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

-- INSERT INTO packages (name, max_uploads_per_month, max_storage_mb)
-- VALUES ('Free', 10, 1024),
--        ('Pro', 100, 10240),
--        ('Gold', 1000, 102400);
