package com.flightstats.hub.model;

import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@EqualsAndHashCode
public class ContentKey {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH/mm/ss/");
    private final LocalDateTime time;
    private final String hash;

    public ContentKey() {
        this(LocalDateTime.now(Clock.systemUTC()), RandomStringUtils.randomAlphanumeric(6));
    }

    ContentKey(LocalDateTime time, String hash) {
        this.time = time;
        this.hash = hash;
    }

    public static Optional<ContentKey> fromString(String key) {
        String[] split = key.split("-");
        long epochMilli = Long.parseLong(split[0]);
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
        return Optional.of(new ContentKey(dateTime, split[1]));
    }

    public String urlKey() {
        return formatter.format(time) + key();
    }

    public String key() {
        return getMillis() + "-" + hash;
    }

    public long getMillis() {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public String toString() {
        return key();
    }
}
