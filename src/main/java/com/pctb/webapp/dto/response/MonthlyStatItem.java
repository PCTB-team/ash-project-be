package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyStatItem {
    String name;
    double value;
    Double users;
    Double revenue;

    public MonthlyStatItem(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public MonthlyStatItem(String name, double value, Double users, Double revenue) {
        this.name = name;
        this.value = value;
        this.users = users;
        this.revenue = revenue;
    }

    public static MonthlyStatItem users(String name, double users) {
        return new MonthlyStatItem(name, users, users, null);
    }

    public static MonthlyStatItem revenue(String name, double revenue) {
        return new MonthlyStatItem(name, revenue, null, revenue);
    }
}
