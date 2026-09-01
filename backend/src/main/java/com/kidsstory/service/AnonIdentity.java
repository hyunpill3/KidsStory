package com.kidsstory.service;

/**
 * No-signup identity: a random cookie plus the request IP.
 * Used to scope a user's own projects and to rate-limit free generations
 * without requiring an account.
 */
public record AnonIdentity(String anonId, String clientIp) {
}
