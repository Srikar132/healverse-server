-- HealVerse Database Schema
-- PostgreSQL Compatible SQL Script
-- Generated: 2025-12-11T11:55:11.944585900

-- Create database (optional)
-- CREATE DATABASE healverse;
-- \c healverse;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create ENUM types
CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER');
CREATE TYPE activity_level_enum AS ENUM ('SEDENTARY', 'LIGHTLY_ACTIVE', 'MODERATELY_ACTIVE', 'VERY_ACTIVE', 'EXTREMELY_ACTIVE');
CREATE TYPE goal_enum AS ENUM ('LOSE_WEIGHT', 'MAINTAIN_WEIGHT', 'GAIN_WEIGHT', 'BUILD_MUSCLE');
CREATE TYPE weight_loss_speed_enum AS ENUM ('SLOW', 'MODERATE', 'FAST');
CREATE TYPE dietary_restriction_enum AS ENUM ('VEGETARIAN', 'VEGAN', 'GLUTEN_FREE', 'KETO', 'NON_VEGETARIAN');
CREATE TYPE health_condition_enum AS ENUM ('NONE', 'DIABETES', 'HYPERTENSION', 'HEART_DISEASE', 'OTHER');
CREATE TYPE message_role_enum AS ENUM ('USER', 'BOT');
CREATE TYPE meal_type_enum AS ENUM ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK');
CREATE TYPE exercise_intensity_enum AS ENUM ('LOW', 'MODERATE', 'HIGH', 'VIGOROUS');
CREATE TYPE medication_type_enum AS ENUM ('TABLET', 'CAPSULE', 'LIQUID', 'INJECTION', 'TOPICAL', 'OTHER');
CREATE TYPE frequency_type_enum AS ENUM ('ONCE_DAILY', 'TWICE_DAILY', 'THREE_TIMES_DAILY', 'FOUR_TIMES_DAILY', 'AS_NEEDED', 'CUSTOM');
CREATE TYPE log_status_enum AS ENUM ('TAKEN', 'MISSED', 'SKIPPED', 'PENDING');

-- Create tables

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    google_id VARCHAR(255) UNIQUE,
    profile_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

