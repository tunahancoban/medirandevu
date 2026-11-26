CREATE TABLE departments (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE doctors (
    id            BIGSERIAL   PRIMARY KEY,
    user_id       BIGINT      NOT NULL UNIQUE,
    department_id BIGINT      NOT NULL REFERENCES departments(id),
    first_name    VARCHAR(50) NOT NULL,
    last_name     VARCHAR(50) NOT NULL,
    title         VARCHAR(50) NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_doctors_department ON doctors(department_id);

CREATE TABLE schedules (
    id           BIGSERIAL   PRIMARY KEY,
    doctor_id    BIGINT      NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week  VARCHAR(10) NOT NULL,
    start_time   TIME        NOT NULL,
    end_time     TIME        NOT NULL,
    slot_minutes INT         NOT NULL,
    CONSTRAINT uq_schedule_doctor_day UNIQUE (doctor_id, day_of_week),
    CONSTRAINT chk_schedule_time      CHECK (end_time > start_time),
    CONSTRAINT chk_schedule_slot      CHECK (slot_minutes BETWEEN 5 AND 120)
);
