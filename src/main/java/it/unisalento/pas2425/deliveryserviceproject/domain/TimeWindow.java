package it.unisalento.pas2425.deliveryserviceproject.domain;

public enum TimeWindow {
    TW_2_HOURS(7200),
    TW_4_HOURS(14400),
    TW_6_HOURS(21600),
    TW_12_HOURS(43200),
    TW_24_HOURS(86400),
    TW_48_HOURS(172800),
    TW_5_DAYS(432000),
    TW_1_WEEK(604800),
    TW_2_WEEKS(1209600);

    private final int seconds;

    TimeWindow(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }
}

