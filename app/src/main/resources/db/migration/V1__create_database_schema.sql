CREATE TABLE registration_tokens
(
    registration_token_pk UUID PRIMARY KEY,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at          TIMESTAMP WITH TIME ZONE NULL
);

CREATE TABLE users
(
    user_pk               BIGSERIAL PRIMARY KEY,
    username              VARCHAR(50)   NOT NULL,
    email                 VARCHAR(256)  NOT NULL,
    first_name            VARCHAR(50)   NOT NULL,
    last_name             VARCHAR(50)   NOT NULL,
    bio                   VARCHAR(1024) NOT NULL,
    profile_picture_fk    UUID          NULL,
    password_hash         VARCHAR(256)  NOT NULL,
    role                  SMALLINT      NOT NULL,
    registration_token_fk UUID          NOT NULL,
    CONSTRAINT users_username_unique_index UNIQUE (username),
    CONSTRAINT users_email_unique_index UNIQUE (email),
    CONSTRAINT users_registration_token_fk_unique_index UNIQUE (registration_token_fk)
);

CREATE TABLE images
(
    image_pk     UUID PRIMARY KEY,
    user_fk      BIGINT                   NOT NULL,
    bucket       UUID                     NOT NULL,
    key          VARCHAR(256)             NOT NULL,
    content_type SMALLINT                 NOT NULL,
    description  VARCHAR(1024)            NOT NULL,
    upload_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT images_bucket_key_unique_index UNIQUE (bucket, key)
);

CREATE TABLE tags
(
    tag_pk   BIGSERIAL PRIMARY KEY,
    name     VARCHAR(50) NOT NULL,
    image_fk UUID        NOT NULL
);

CREATE TABLE social_media_links
(
    social_media_link_pk BIGSERIAL PRIMARY KEY,
    user_fk              BIGINT       NOT NULL,
    platform             VARCHAR(100) NOT NULL,
    url                  VARCHAR(256) NOT NULL
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
    expires_at              TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at                 TIMESTAMP WITH TIME ZONE NULL
);

ALTER TABLE users
    ADD FOREIGN KEY (registration_token_fk) REFERENCES registration_tokens (registration_token_pk);

ALTER TABLE users
    ADD FOREIGN KEY (profile_picture_fk) REFERENCES images (image_pk);

ALTER TABLE images
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE tags
    ADD FOREIGN KEY (image_fk) REFERENCES images (image_pk) ON DELETE CASCADE;

ALTER TABLE social_media_links
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE refresh_tokens
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;

ALTER TABLE password_reset_tokens
    ADD FOREIGN KEY (user_fk) REFERENCES users (user_pk) ON DELETE CASCADE;
