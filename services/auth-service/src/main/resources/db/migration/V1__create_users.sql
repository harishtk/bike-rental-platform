CREATE TABLE users (
       id UUID PRIMARY KEY,
       username VARCHAR(100) NOT NULL UNIQUE,
       password VARCHAR(255) NOT NULL,
       role VARCHAR(30) NOT NULL,
       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

       CONSTRAINT chk_user_role
           CHECK (role IN ('USER', 'ADMIN'))
);

CREATE UNIQUE INDEX uq_users_username
    ON users (username);