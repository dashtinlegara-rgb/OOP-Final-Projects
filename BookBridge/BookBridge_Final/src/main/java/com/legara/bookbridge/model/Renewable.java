package com.legara.bookbridge.model;

import java.time.LocalDate;

public interface Renewable {
    boolean renew(int additionalDays, LocalDate today);
}
