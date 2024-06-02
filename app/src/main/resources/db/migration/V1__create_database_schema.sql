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
    username              VARCHAR(50)  NOT NULL,
    email                 VARCHAR(256) NOT NULL,
    password_hash         VARCHAR(256) NOT NULL,
    role                  SMALLINT     NOT NULL,
    registration_token_fk UUID         NOT NULL,
    FOREIGN KEY (registration_token_fk) REFERENCES registration_tokens (registration_token_pk),
    CONSTRAINT users_username_unique_index UNIQUE (username),
    CONSTRAINT users_email_unique_index UNIQUE (email),
    CONSTRAINT users_registration_token_fk_unique_index UNIQUE (registration_token_fk)
);

CREATE TABLE refresh_tokens
(
    refresh_token_pk UUID PRIMARY KEY,
    user_fk          BIGINT                   NOT NULL,
    issued_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at       TIMESTAMP WITH TIME ZONE NULL,
    FOREIGN KEY (user_fk) REFERENCES users (user_pk)
);
