package de.jpx3.intave.version;

import java.util.concurrent.TimeUnit;

public final class DurationTranslator {
  public static String translateMinutes(long duration) {
    if (duration < 0) {
      return "无效";
    }
    if (duration == 0) {
      return "0 分钟";
    }
    int minutes = (int) (duration / (1000 * 60));
    int hours = minutes / 60;
    minutes = minutes % 60;
    String firstType = stringifyType(TimeUnit.HOURS, hours);
    String secondType = stringifyType(TimeUnit.MINUTES, minutes);
    if (secondType.isEmpty()) {
      secondType = "0 分钟";
    }
    String output;
    if (hours >= 24) {
      output = firstType;
    } else {
      output = firstType + (firstType.isEmpty() ? "" : " ") + secondType;
    }
    if (output.trim().isEmpty()) {
      output = "几分钟";
    }
    return output;
  }

  public static String translateHours(long duration) {
    if (duration <= 0) {
      return "无效";
    }
    int hours = (int) (duration / (1000 * 60 * 60));
    int days = hours / 24;
    hours = hours % 24;
    String firstType = stringifyType(TimeUnit.DAYS, days);
    String secondType = stringifyType(TimeUnit.HOURS, hours);
    if (secondType.isEmpty()) {
      secondType = "0 小时";
    }
    String output;
    if (days >= 7) {
      output = firstType;
    } else {
      output = firstType + (firstType.isEmpty() ? "" : " ") + secondType;
    }
    if (output.trim().isEmpty()) {
      output = "几小时";
    }
    return output;
  }

  private static String stringifyType(TimeUnit unit, long conv) {
    if (conv == 0) {
      return "";
    }
    String unitName;
    if (unit == TimeUnit.DAYS) {
      unitName = "天";
    } else if (unit == TimeUnit.HOURS) {
      unitName = "小时";
    } else if (unit == TimeUnit.MINUTES) {
      unitName = "分钟";
    } else {
      unitName = unit.name().toLowerCase();
    }
    return conv + " " + unitName;
  }
}
